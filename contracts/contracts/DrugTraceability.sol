// SPDX-License-Identifier: MIT
pragma solidity 0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/utils/Pausable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import "@openzeppelin/contracts/utils/cryptography/MerkleProof.sol";

/**
 * @title DrugTraceability
 * @dev Smart Contract tối ưu cho hệ thống truy xuất nguồn gốc thuốc
 * 
 * @author Quyen
 */
contract DrugTraceability is ERC721, AccessControl, Pausable, ReentrancyGuard {
    
    // ============================================================
    // ROLES
    // ============================================================
    bytes32 public constant MANUFACTURER_ROLE = keccak256("MANUFACTURER_ROLE");
    bytes32 public constant PHARMACY_ROLE = keccak256("PHARMACY_ROLE");
    bytes32 public constant DISTRIBUTOR_ROLE = keccak256("DISTRIBUTOR_ROLE");
    bytes32 public constant ADMIN_ROLE = keccak256("ADMIN_ROLE");
    
    // ============================================================
    // COUNTERS
    // ============================================================
    uint256 private _batchIdCounter;
    uint256 private _shipmentIdCounter;
    
    // ============================================================
    // ENUMS
    // ============================================================
    enum BatchStatus {
        ACTIVE,           // Đang hoạt động
        EXPIRED,          // Hết hạn
        RECALLED          // Thu hồi
    }

    // Trạng thái của từng hộp thuốc (Item)
    enum ItemStatus {
        AVAILABLE,        // Có thể bán
        SOLD,             // Đã bán cho khách
        RECALLED,         // Bị thu hồi
        DAMAGED           // Hư hỏng
    }
    
    // ============================================================
    // STRUCTS
    // ============================================================
    
    struct DrugInfo {
        string name;                // Tên thuốc
        string activeIngredient;    // Hoạt chất
        string dosage;              // Liều lượng
        string manufacturer;        // Tên nhà sản xuất
        string registrationNumber;  // Số đăng ký
    }
    
    struct Batch {
        uint256 batchId;            // ID lô
        DrugInfo drugInfo;          // Thông tin thuốc
        uint256 quantity;           // Số lượng hộp
        uint256 manufactureDate;    // Ngày sản xuất
        uint256 expiryDate;         // Ngày hết hạn
        address manufacturer;       // Địa chỉ nhà sản xuất
        address currentOwner;       // Chủ sở hữu hiện tại (cấp lô)
        BatchStatus status;         // Trạng thái lô
        bytes32 itemsMerkleRoot;    // Merkle root chứng thực danh sách mã QR
        bool exists;                // Batch có tồn tại không
    }
    
    struct Shipment {
        uint256 shipmentId;         // ID shipment
        uint256 batchId;            // ID lô thuốc
        address fromAddress;        // Người gửi
        address toAddress;          // Người nhận
        string fromLocation;        // Địa điểm gửi
        string toLocation;          // Địa điểm nhận
        uint256 quantity;           // Số lượng
        uint256 shipDate;           // Ngày gửi
        uint256 receiveDate;        // Ngày nhận (0 nếu chưa nhận)
        bool isReceived;            // Đã nhận hay chưa
        string trackingNumber;      // Số tracking
        string notes;               // Ghi chú
    }
    
    struct JourneyPoint {
        address location;           // Địa chỉ ví
        string locationType;        // "MANUFACTURER", "DISTRIBUTOR", "PHARMACY"
        string locationName;        // Tên cơ sở thực tế
        uint256 timestamp;          // Thời gian
        uint256 shipmentId;         // Shipment ID (0 nếu là điểm khởi đầu)
    }
    
    // ============================================================
    // STATE VARIABLES
    // ============================================================
    
    // Batch mappings
    mapping(uint256 => Batch) public batches;
    mapping(address => uint256[]) public manufacturerBatches;
    
    // Shipment mappings
    mapping(uint256 => Shipment) public shipments;
    mapping(uint256 => uint256[]) public batchShipments;
    
    // Journey tracking
    mapping(uint256 => JourneyPoint[]) public batchJourney;
    
    // ITEM LEVEL TRACKING (Mới)
    // Lưu trạng thái của từng hộp thuốc: hash(batchId, itemCode) => ItemStatus
    // Chỉ lưu những item có trạng thái đặc biệt (SOLD, RECALLED) để tiết kiệm gas.
    // Mặc định là AVAILABLE.
    mapping(bytes32 => ItemStatus) public itemStatuses;
    
    // ============================================================
    // EVENTS (Decoded Logs)
    // ============================================================
    
    event BatchCreated(
        uint256 indexed batchId,
        address indexed manufacturer,
        string drugName,
        uint256 quantity,
        uint256 manufactureDate,
        uint256 expiryDate,
        bytes32 itemsMerkleRoot,
        uint256 timestamp
    );
    
    event ShipmentCreatedAndDispatched(
        uint256 indexed shipmentId,
        uint256 indexed batchId,
        address indexed from,
        address to,
        string fromLocation,
        string toLocation,
        uint256 quantity,
        uint256 shipDate,
        string trackingNumber
    );
    
    event ShipmentReceived(
        uint256 indexed shipmentId,
        uint256 indexed batchId,
        address indexed receiver,
        uint256 receiveDate,
        address previousOwner
    );
    
    event BatchStatusChanged(
        uint256 indexed batchId,
        BatchStatus oldStatus,
        BatchStatus newStatus,
        uint256 timestamp
    );
    
    event OwnershipTransferred(
        uint256 indexed batchId,
        address indexed from,
        address indexed to,
        uint256 timestamp
    );

    // Event cho từng hộp thuốc (Quan trọng cho minh bạch người dùng cuối)
    event ItemSold(
        uint256 indexed batchId,
        string itemCode,
        address indexed pharmacy,
        uint256 timestamp
    );

    event ItemStatusUpdated(
        uint256 indexed batchId,
        string itemCode,
        ItemStatus status,
        string reason,
        uint256 timestamp
    );
    
    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    
    constructor() ERC721("DrugTraceability", "DRUG") {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(ADMIN_ROLE, msg.sender);
    }
    
    // ============================================================
    // MODIFIERS
    // ============================================================
    
    modifier batchExists(uint256 batchId) {
        require(batches[batchId].exists, "Batch does not exist");
        _;
    }
    
    modifier onlyBatchOwner(uint256 batchId) {
        require(batches[batchId].currentOwner == msg.sender, "Not batch owner");
        _;
    }
    
    // ============================================================
    // CORE FUNCTIONS - BATCH LEVEL
    // ============================================================
    
    function createBatchWithItems(
        DrugInfo memory _drugInfo,
        uint256 _quantity,
        uint256 _manufactureDate,
        uint256 _expiryDate,
        bytes32 _itemsMerkleRoot,
        string memory _manufacturerName
    ) external onlyRole(MANUFACTURER_ROLE) whenNotPaused returns (uint256) {
        require(_quantity > 0, "Quantity must be > 0");
        require(_expiryDate > _manufactureDate, "Invalid expiry date");
        require(_itemsMerkleRoot != bytes32(0), "Invalid merkle root");
        
        _batchIdCounter++;
        uint256 newBatchId = _batchIdCounter;
        
        Batch storage newBatch = batches[newBatchId];
        newBatch.batchId = newBatchId;
        newBatch.drugInfo = _drugInfo;
        newBatch.quantity = _quantity;
        newBatch.manufactureDate = _manufactureDate;
        newBatch.expiryDate = _expiryDate;
        newBatch.manufacturer = msg.sender;
        newBatch.currentOwner = msg.sender;
        newBatch.status = BatchStatus.ACTIVE;
        newBatch.itemsMerkleRoot = _itemsMerkleRoot;
        newBatch.exists = true;
        
        manufacturerBatches[msg.sender].push(newBatchId);
        
        batchJourney[newBatchId].push(JourneyPoint({
            location: msg.sender,
            locationType: "MANUFACTURER",
            locationName: _manufacturerName,
            timestamp: block.timestamp,
            shipmentId: 0
        }));
        
        _safeMint(msg.sender, newBatchId);
        
        emit BatchCreated(
            newBatchId,
            msg.sender,
            _drugInfo.name,
            _quantity,
            _manufactureDate,
            _expiryDate,
            _itemsMerkleRoot,
            block.timestamp
        );
        
        return newBatchId;
    }
    
    function createAndDispatchShipment(
        uint256 _batchId,
        address _toAddress,
        string memory _fromLocation,
        string memory _toLocation,
        string memory /* _toLocationType */,
        uint256 _quantity,
        string memory _trackingNumber,
        string memory _notes
    ) external onlyBatchOwner(_batchId) batchExists(_batchId) whenNotPaused nonReentrant returns (uint256) {
        require(_toAddress != address(0), "Invalid recipient address");
        require(_quantity > 0 && _quantity <= batches[_batchId].quantity, "Invalid quantity");
        require(batches[_batchId].status == BatchStatus.ACTIVE, "Batch not active");
        
        require(
            hasRole(DISTRIBUTOR_ROLE, _toAddress) || hasRole(PHARMACY_ROLE, _toAddress),
            "Recipient must be distributor or pharmacy"
        );
        
        _shipmentIdCounter++;
        uint256 newShipmentId = _shipmentIdCounter;
        
        Shipment storage newShipment = shipments[newShipmentId];
        newShipment.shipmentId = newShipmentId;
        newShipment.batchId = _batchId;
        newShipment.fromAddress = msg.sender;
        newShipment.toAddress = _toAddress;
        newShipment.fromLocation = _fromLocation;
        newShipment.toLocation = _toLocation;
        newShipment.quantity = _quantity;
        newShipment.shipDate = block.timestamp;
        newShipment.receiveDate = 0;
        newShipment.isReceived = false;
        newShipment.trackingNumber = _trackingNumber;
        newShipment.notes = _notes;
        
        batchShipments[_batchId].push(newShipmentId);
        
        emit ShipmentCreatedAndDispatched(
            newShipmentId,
            _batchId,
            msg.sender,
            _toAddress,
            _fromLocation,
            _toLocation,
            _quantity,
            block.timestamp,
            _trackingNumber
        );
        
        return newShipmentId;
    }
    
    function receiveShipment(
        uint256 _shipmentId,
        string memory _receiverLocationName
    ) external whenNotPaused nonReentrant {
        Shipment storage shipment = shipments[_shipmentId];
        
        require(shipment.shipmentId != 0, "Shipment does not exist");
        require(shipment.toAddress == msg.sender, "Not authorized to receive");
        require(!shipment.isReceived, "Shipment already received");
        
        uint256 batchId = shipment.batchId;
        Batch storage batch = batches[batchId];
        
        require(batch.exists, "Batch does not exist");
        
        shipment.isReceived = true;
        shipment.receiveDate = block.timestamp;
        
        address previousOwner = batch.currentOwner;
        batch.currentOwner = msg.sender;
        
        string memory locationType;
        if (hasRole(DISTRIBUTOR_ROLE, msg.sender)) {
            locationType = "DISTRIBUTOR";
        } else if (hasRole(PHARMACY_ROLE, msg.sender)) {
            locationType = "PHARMACY";
        } else {
            locationType = "UNKNOWN";
        }
        
        batchJourney[batchId].push(JourneyPoint({
            location: msg.sender,
            locationType: locationType,
            locationName: _receiverLocationName,
            timestamp: block.timestamp,
            shipmentId: _shipmentId
        }));
        
        emit ShipmentReceived(
            _shipmentId,
            batchId,
            msg.sender,
            block.timestamp,
            previousOwner
        );
        
        emit OwnershipTransferred(
            batchId,
            previousOwner,
            msg.sender,
            block.timestamp
        );
    }

    // ============================================================
    // CORE FUNCTIONS - ITEM LEVEL (QUAN TRỌNG CHO MINH BẠCH)
    // ============================================================

    /**
     * @dev Xác thực một hộp thuốc có thuộc lô này không.
     * Dùng Merkle Proof để kiểm tra off-chain item code có khớp với on-chain Merkle Root không.
     * 
     * @param _batchId ID của lô thuốc
     * @param _itemCode Mã QR của hộp thuốc (String)
     * @param _merkleProof Mảng các hash chứng thực (Lấy từ Backend)
     * @return isValid True nếu hộp thuốc hợp lệ
     * @return currentStatus Trạng thái hiện tại của hộp thuốc
     */
    function verifyItem(
        uint256 _batchId,
        string memory _itemCode,
        bytes32[] calldata _merkleProof
    ) external view batchExists(_batchId) returns (bool isValid, ItemStatus currentStatus) {
        Batch memory batch = batches[_batchId];
        
        // 1. Tính hash của item code (Leaf node)
        // Lưu ý: Backend phải hash giống hệt cách này (keccak256(bytes(itemCode)))
        bytes32 leaf = keccak256(bytes(_itemCode));
        
        // 2. Verify Merkle Proof
        bool isPartOfBatch = MerkleProof.verify(_merkleProof, batch.itemsMerkleRoot, leaf);
        
        if (!isPartOfBatch) {
            return (false, ItemStatus.AVAILABLE); // Trạng thái không quan trọng nếu không hợp lệ
        }
        
        // 3. Lấy trạng thái hiện tại
        bytes32 itemHash = keccak256(abi.encodePacked(_batchId, _itemCode));
        ItemStatus status = itemStatuses[itemHash];
        
        return (true, status);
    }

    /**
     * @dev Bán một hộp thuốc (Chỉ Hiệu thuốc mới gọi được).
     * Đánh dấu item là SOLD trên Blockchain.
     * 
     * @param _batchId ID lô thuốc
     * @param _itemCode Mã QR hộp thuốc
     * @param _merkleProof Proof để chứng minh item thuộc batch này (tránh spam item rác)
     */
    function sellItem(
        uint256 _batchId,
        string memory _itemCode,
        bytes32[] calldata _merkleProof
    ) external onlyRole(PHARMACY_ROLE) batchExists(_batchId) whenNotPaused {
        // 1. Verify quyền sở hữu batch
        require(batches[_batchId].currentOwner == msg.sender, "Pharmacy must own the batch");
        
        // 2. Verify item thuộc batch
        bytes32 leaf = keccak256(bytes(_itemCode));
        require(MerkleProof.verify(_merkleProof, batches[_batchId].itemsMerkleRoot, leaf), "Invalid item proof");
        
        // 3. Verify trạng thái
        bytes32 itemHash = keccak256(abi.encodePacked(_batchId, _itemCode));
        require(itemStatuses[itemHash] == ItemStatus.AVAILABLE, "Item already sold or not available");
        
        // 4. Cập nhật trạng thái
        itemStatuses[itemHash] = ItemStatus.SOLD;
        
        emit ItemSold(_batchId, _itemCode, msg.sender, block.timestamp);
    }

    /**
     * @dev Cập nhật trạng thái item (Dùng cho Thu hồi hoặc Báo hỏng)
     */
    function updateItemStatus(
        uint256 _batchId,
        string memory _itemCode,
        ItemStatus _newStatus,
        string memory _reason,
        bytes32[] calldata _merkleProof
    ) external batchExists(_batchId) {
        // Chỉ owner hiện tại hoặc Admin mới được update
        require(
            batches[_batchId].currentOwner == msg.sender || hasRole(ADMIN_ROLE, msg.sender),
            "Not authorized"
        );
        
        bytes32 leaf = keccak256(bytes(_itemCode));
        require(MerkleProof.verify(_merkleProof, batches[_batchId].itemsMerkleRoot, leaf), "Invalid item proof");
        
        bytes32 itemHash = keccak256(abi.encodePacked(_batchId, _itemCode));
        itemStatuses[itemHash] = _newStatus;
        
        emit ItemStatusUpdated(_batchId, _itemCode, _newStatus, _reason, block.timestamp);
    }
    
    // ============================================================
    // QUERY FUNCTIONS
    // ============================================================
    
    function getBatchFullTraceability(uint256 _batchId) 
        external 
        view 
        batchExists(_batchId)
        returns (
            Batch memory batch,
            JourneyPoint[] memory journey,
            uint256[] memory shipmentIds
        ) 
    {
        return (
            batches[_batchId],
            batchJourney[_batchId],
            batchShipments[_batchId]
        );
    }
    
    function getShipmentDetails(uint256 _shipmentId) external view returns (Shipment memory) {
        require(shipments[_shipmentId].shipmentId != 0, "Shipment does not exist");
        return shipments[_shipmentId];
    }
    
    function getManufacturerBatches(address _manufacturer) external view returns (uint256[] memory) {
        return manufacturerBatches[_manufacturer];
    }
    
    function getBatchesByOwner(address _owner) 
        external 
        view 
        returns (
            uint256[] memory batchIds,
            uint256[] memory quantities,
            BatchStatus[] memory statuses
        ) 
    {
        uint256 totalBatches = _batchIdCounter;
        uint256 count = 0;
        
        for (uint256 i = 1; i <= totalBatches; i++) {
            if (batches[i].exists && batches[i].currentOwner == _owner) {
                count++;
            }
        }
        
        batchIds = new uint256[](count);
        quantities = new uint256[](count);
        statuses = new BatchStatus[](count);
        
        uint256 index = 0;
        for (uint256 i = 1; i <= totalBatches; i++) {
            if (batches[i].exists && batches[i].currentOwner == _owner) {
                batchIds[index] = batches[i].batchId;
                quantities[index] = batches[i].quantity;
                statuses[index] = batches[i].status;
                index++;
            }
        }
        
        return (batchIds, quantities, statuses);
    }
    
    // ============================================================
    // ADMIN FUNCTIONS
    // ============================================================
    
    function addManufacturer(address _manufacturer) external onlyRole(ADMIN_ROLE) {
        _grantRole(MANUFACTURER_ROLE, _manufacturer);
    }
    
    function addDistributor(address _distributor) external onlyRole(ADMIN_ROLE) {
        _grantRole(DISTRIBUTOR_ROLE, _distributor);
    }
    
    function addPharmacy(address _pharmacy) external onlyRole(ADMIN_ROLE) {
        _grantRole(PHARMACY_ROLE, _pharmacy);
    }
    
    function recallBatch(uint256 _batchId) external onlyRole(ADMIN_ROLE) batchExists(_batchId) {
        Batch storage batch = batches[_batchId];
        BatchStatus oldStatus = batch.status;
        batch.status = BatchStatus.RECALLED;
        emit BatchStatusChanged(_batchId, oldStatus, BatchStatus.RECALLED, block.timestamp);
    }
    
    function pause() external onlyRole(ADMIN_ROLE) {
        _pause();
    }
    
    function unpause() external onlyRole(ADMIN_ROLE) {
        _unpause();
    }
    
    // ============================================================
    // SOUL-BOUND TOKEN IMPLEMENTATION
    // ============================================================
    
    function _update(address to, uint256 tokenId, address auth) internal override returns (address) {
        address from = _ownerOf(tokenId);
        require(from == address(0) || to == address(0), "Soul-bound token cannot be transferred");
        return super._update(to, tokenId, auth);
    }
    
    function approve(address, uint256) public pure override {
        revert("Soul-bound token cannot be approved");
    }
    
    function setApprovalForAll(address, bool) public pure override {
        revert("Soul-bound token cannot be approved");
    }
    
    function supportsInterface(bytes4 interfaceId) public view override(ERC721, AccessControl) returns (bool) {
        return super.supportsInterface(interfaceId);
    }
}
