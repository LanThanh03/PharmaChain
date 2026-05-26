package com.nckh.dia5.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

@Slf4j
@Configuration
public class BlockchainConfig {

    @Value("${blockchain.network.url}")
    private String blockchainNetworkUrl;

    @Value("${blockchain.network.chain-id}")
    private Long chainId;

    @Value("${pharmaledger.contract.address}")
    private String contractAddress;

    @Value("${blockchain.wallet.private-key}")
    private String privateKey;

    @Value("${blockchain.gas.price:20000000000}")
    private BigInteger gasPrice;

    @Value("${blockchain.gas.limit:6721975}")
    private BigInteger gasLimit;

    @Bean
    public Web3j web3j() {
        try {
            log.info("Connecting to blockchain network: {}", blockchainNetworkUrl);
            
            // Create HttpService with timeout settings
            okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            
            HttpService httpService = new HttpService(blockchainNetworkUrl, httpClient);
            Web3j web3j = Web3j.build(httpService);
            
            // Test connection (non-blocking for startup)
            try {
                String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
                log.info("Successfully connected to blockchain. Client version: {}", clientVersion);
            } catch (Exception testException) {
                log.warn("Blockchain connection test failed, but proceeding with startup: {}", testException.getMessage());
            }
            
            return web3j;
        } catch (Exception e) {
            log.error("Failed to initialize blockchain connection: {}", blockchainNetworkUrl, e);
            // Return a Web3j instance anyway to allow startup
            okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            return Web3j.build(new HttpService(blockchainNetworkUrl, httpClient));
        }
    }

    @Bean
    public Credentials credentials() {
        try {
            if (privateKey == null || privateKey.isEmpty() || privateKey.equals("0x0000000000000000000000000000000000000000000000000000000000000000")) {
                log.warn("No valid private key provided. Some blockchain operations may not work.");
                return null;
            }
            
            Credentials credentials = Credentials.create(privateKey);
            log.info("Blockchain credentials loaded for address: {}", credentials.getAddress());
            return credentials;
        } catch (Exception e) {
            log.error("Failed to load blockchain credentials", e);
            throw new RuntimeException("Cannot load blockchain credentials", e);
        }
    }

    @Bean
    public ContractGasProvider gasProvider() {
        return new DefaultGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                return gasPrice;
            }

            @Override
            public BigInteger getGasPrice() {
                return gasPrice;
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                return gasLimit;
            }

            @Override
            public BigInteger getGasLimit() {
                return gasLimit;
            }
        };
    }

    // Getters for configuration values
    public String getBlockchainNetworkUrl() {
        return blockchainNetworkUrl;
    }

    public Long getChainId() {
        return chainId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }
}
