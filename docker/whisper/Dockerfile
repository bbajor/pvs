FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY src/main/resources/ai/whisper_server.py /app/
RUN pip install --no-cache-dir \
    flask \
    flask-cors \
    requests \
    faster-whisper \
    gunicorn

# Expose port
EXPOSE 9000

# Health check script
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:9000/health')"

# Run the server with a production WSGI server
CMD ["gunicorn", "-w", "2", "-b", "0.0.0.0:9000", "whisper_server:app"]
