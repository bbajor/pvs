#!/usr/bin/env python3
"""
Test script for Whisper container
Usage: python test-whisper.py [host] [port]
"""

import sys
import requests
import os

WHISPER_HOST = sys.argv[1] if len(sys.argv) > 1 else 'localhost'
WHISPER_PORT = sys.argv[2] if len(sys.argv) > 2 else '8000'
WHISPER_URL = f"http://{WHISPER_HOST}:{WHISPER_PORT}"

print(f"Testing Whisper container at {WHISPER_URL}")

# Test 1: Health check
print("\n=== Test 1: Health Check ===")
try:
    response = requests.get(f"{WHISPER_URL}/health", timeout=5)
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
    if response.status_code == 200:
        print("✓ Health check passed\n")
    else:
        print("✗ Health check failed\n")
        sys.exit(1)
except requests.exceptions.ConnectionError:
    print("✗ Cannot connect to Whisper container")
    print("\nPlease check if Whisper container is running:")
    print("  docker ps | grep whisper")
    sys.exit(1)
except Exception as e:
    print(f"✗ Error: {e}")
    sys.exit(1)

# Test 2: Transcription (if audio file provided)
if len(sys.argv) > 3:
    audio_file = sys.argv[3]
    print(f"\n=== Test 2: Transcription Test ===")
    print(f"Uploading file: {audio_file}")
    
    if not os.path.exists(audio_file):
        print(f"✗ File not found: {audio_file}")
        sys.exit(1)
    
    try:
        with open(audio_file, 'rb') as f:
            files = {'audio': (os.path.basename(audio_file), f, 'audio/webm')}
            response = requests.post(f"{WHISPER_URL}/transcribe", files=files, timeout=120)
        
        print(f"Status: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            result = response.json()
            if 'text' in result:
                print(f"✓ Transcription successful!")
                print(f"Text: {result['text']}")
            else:
                print("✗ No text in response")
                if 'error' in result:
                    print(f"Error: {result['error']}")
        else:
            print("✗ Transcription failed")
    except Exception as e:
        print(f"✗ Error: {e}")
        import traceback
        traceback.print_exc()
else:
    print("\n=== Test 2: Transcription ===")
    print("To test transcription, provide an audio file:")
    print(f"  python test-whisper.py {WHISPER_HOST} {WHISPER_PORT} audio.webm")

print("\n=== Test Complete ===")

