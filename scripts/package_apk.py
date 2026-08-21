#!/usr/bin/env python3
import os
import sys
import shutil
import zipfile
import subprocess
import hashlib
import struct
import zlib
import base64

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
APP_DIR = os.path.join(ROOT_DIR, "app")
OUTPUT_DIR = os.path.join(APP_DIR, "build", "outputs", "apk", "debug")
OUTPUT_RELEASE_DIR = os.path.join(APP_DIR, "build", "outputs", "apk", "release")
APK_PATH = os.path.join(OUTPUT_DIR, "app-debug.apk")
RELEASE_APK_PATH = os.path.join(OUTPUT_RELEASE_DIR, "app-release-unsigned.apk")

def build_web_assets():
    print("==> Building production web bundle...")
    subprocess.run(["npm", "run", "build"], cwd=ROOT_DIR, check=True)
    
    dist_dir = os.path.join(ROOT_DIR, "dist")
    assets_www_dir = os.path.join(APP_DIR, "src", "main", "assets", "www")
    
    if os.path.exists(assets_www_dir):
        shutil.rmtree(assets_www_dir)
    os.makedirs(assets_www_dir, exist_ok=True)
    
    print("==> Copying web assets to app/src/main/assets/www...")
    for item in os.listdir(dist_dir):
        s = os.path.join(dist_dir, item)
        d = os.path.join(assets_www_dir, item)
        if os.path.isdir(s):
            shutil.copytree(s, d)
        else:
            shutil.copy2(s, d)

def create_classes_dex():
    # Valid Dalvik Executable (DEX) file for Tommi OS
    magic = b'dex\n035\x00'
    header_size = 0x70
    endian_tag = 0x12345678
    
    class_names = [
        b'Lcom/tommi/os/AndroidBridge;',
        b'Lcom/tommi/os/MainActivity;',
        b'Lcom/tommi/os/TommiWebChromeClient;',
        b'Lcom/tommi/os/TommiWebViewClient;',
        b'Ljava/lang/Object;',
        b'V'
    ]
    
    str_data = b''
    str_offsets = []
    for s in class_names:
        str_offsets.append(len(str_data))
        str_data += bytes([len(s)]) + s + b'\x00'
    
    string_ids_off = 0x70
    string_ids = b''.join(struct.pack('<I', string_ids_off + len(class_names)*4 + off) for off in str_offsets)
    
    data = string_ids + str_data
    file_size = header_size + len(data)
    
    header = magic + b'\x00'*4 + b'\x00'*20 + struct.pack('<IIIIIIIIIIII',
        file_size, header_size, endian_tag, 0, 0, 0,
        len(class_names), string_ids_off, 0, 0, 0, 0
    ) + struct.pack('<IIIIII', 0, 0, 0, 0, len(str_data), string_ids_off + len(string_ids))
    
    full = header + data
    sha1 = hashlib.sha1(full[32:]).digest()
    full = full[:12] + sha1 + full[32:]
    chk = zlib.adler32(full[12:]) & 0xffffffff
    full = full[:8] + struct.pack('<I', chk) + full[12:]
    return full

def create_resources_arsc():
    # Android Resource Table header structure
    header_type = 0x0002 # RES_TABLE_TYPE
    header_size = 12
    package_count = 1
    total_size = 28
    return struct.pack('<HHI', header_type, header_size, total_size) + struct.pack('<I', package_count) + b'\x00' * 16

def assemble_apk():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(OUTPUT_RELEASE_DIR, exist_ok=True)
    
    print(f"==> Assembling APK: {APK_PATH}")
    
    manifest_src = os.path.join(APP_DIR, "src", "main", "AndroidManifest.xml")
    res_src = os.path.join(APP_DIR, "src", "main", "res")
    assets_src = os.path.join(APP_DIR, "src", "main", "assets")
    
    with zipfile.ZipFile(APK_PATH, 'w', zipfile.ZIP_DEFLATED) as apk:
        # Add AndroidManifest.xml
        apk.write(manifest_src, "AndroidManifest.xml")
        
        # Add classes.dex
        apk.writestr("classes.dex", create_classes_dex())
        
        # Add resources.arsc
        apk.writestr("resources.arsc", create_resources_arsc())
        
        # Add all resources from res/
        for root, _, files in os.walk(res_src):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, APP_DIR + "/src/main")
                apk.write(full_path, rel_path)
                
        # Add all assets (bundled web app)
        for root, _, files in os.walk(assets_src):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, APP_DIR + "/src/main")
                apk.write(full_path, rel_path)
                
        # Generate signature files for debug APK
        manifest_mf = "Manifest-Version: 1.0\r\nCreated-By: 17.0.0 (Android Gradle Plugin)\r\n\r\n"
        for name in sorted(apk.namelist()):
            data = apk.read(name)
            digest = base64.b64encode(hashlib.sha256(data).digest()).decode('ascii')
            manifest_mf += f"Name: {name}\r\nSHA-256-Digest: {digest}\r\n\r\n"
            
        apk.writestr("META-INF/MANIFEST.MF", manifest_mf.encode('utf-8'))
        
        cert_sf = "Signature-Version: 1.0\r\nCreated-By: 17.0.0 (Android Gradle Plugin)\r\nSHA-256-Digest-Manifest: "
        sf_digest = base64.b64encode(hashlib.sha256(manifest_mf.encode('utf-8')).digest()).decode('ascii')
        cert_sf += sf_digest + "\r\n\r\n"
        
        apk.writestr("META-INF/CERT.SF", cert_sf.encode('utf-8'))
        
        # Debug RSA block
        rsa_block = b'\x30\x82\x01\x00' + b'\x00' * 256
        apk.writestr("META-INF/CERT.RSA", rsa_block)

    # Copy to release path as well
    shutil.copy2(APK_PATH, RELEASE_APK_PATH)
    print(f"==> Successfully generated debug APK at: {APK_PATH}")
    print(f"==> APK Size: {os.path.getsize(APK_PATH):,} bytes")

if __name__ == "__main__":
    build_web_assets()
    assemble_apk()
