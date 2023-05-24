import time

from Indexer import indexData



if __name__ == "__main__":

 while True:

        # Process the data or perform any necessary operations
        res = indexData()

        responseMessage = ""
        if not res:
            responseMessage = "All data is already in the store."
        else:
            # Send a response back to the client
            responseMessage = "Indexing has been done successfully."

        print(responseMessage)
        time.sleep(300)
