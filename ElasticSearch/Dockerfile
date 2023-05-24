FROM python:3.9

WORKDIR /app

COPY requirment.txt /app
RUN pip install --no-cache-dir -r requirment.txt

COPY Indexer.py /app
COPY server.py /app

EXPOSE 8080

CMD ["python", "server.py"]
