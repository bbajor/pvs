#!/usr/bin/env python3
"""
Minimal Flask server for Faster-Whisper transcription.
DSGVO-compliant local processing.
"""
import os
import sys
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Global model variable
model = None

def load_model():
    """Lazy load the Whisper model on first request."""
    global model
    if model is None:
        try:
            from faster_whisper import WhisperModel
            # Use base model for balance between speed and accuracy
            model = WhisperModel("base", device="cpu", compute_type="int8")
            print("Whisper model loaded successfully")
        except Exception as e:
            print(f"Error loading Whisper model: {e}", file=sys.stderr)
            raise
    return model

@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    # Return OK immediately - model will be loaded on first transcription request
    # This allows the container to be marked as healthy quickly
    model_status = "loaded" if model is not None else "not_loaded"
    return jsonify({"status": "ok", "model": model_status, "ready": True})

@app.route("/transcribe", methods=["POST"])
def transcribe():
    """Transcribe audio file."""
    try:
        whisper_model = load_model()
        
        if 'audio' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400
        
        audio_file = request.files['audio']
        filename = audio_file.filename or 'audio.webm'
        
        # Determine file extension from filename or content type
        import tempfile
        suffix = '.webm'  # default
        if filename.lower().endswith('.webm'):
            suffix = '.webm'
        elif filename.lower().endswith('.wav'):
            suffix = '.wav'
        elif filename.lower().endswith('.mp3'):
            suffix = '.mp3'
        elif filename.lower().endswith('.m4a'):
            suffix = '.m4a'
        elif filename.lower().endswith('.ogg'):
            suffix = '.ogg'
        
        # Save temporarily to process - faster-whisper can handle various formats
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp_file:
            audio_file.save(tmp_file.name)
            tmp_path = tmp_file.name
        
        try:
            # Transcribe - faster-whisper handles format conversion internally via ffmpeg
            segments, info = whisper_model.transcribe(tmp_path, language="de")
            text = " ".join([segment.text for segment in segments]).strip()
            
            return jsonify({
                "text": text,
                "language": info.language,
                "duration": info.duration
            })
        finally:
            # Clean up temp file
            if os.path.exists(tmp_path):
                try:
                    os.remove(tmp_path)
                except:
                    pass  # Ignore cleanup errors
                
    except Exception as e:
        print(f"Transcription error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 9000))
    app.run(host="0.0.0.0", port=port, debug=False)

