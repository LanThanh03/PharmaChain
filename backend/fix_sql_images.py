import os

file_path = r'c:\Users\Admin\Documents\Workspace\DoAnTotNghiepBC\backend\import_products.sql'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('https://cdn.nhathuoclongchau.com.vn/unsafe/800x0/', '')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Replaced Longchau unsafe prefix in {file_path}")
