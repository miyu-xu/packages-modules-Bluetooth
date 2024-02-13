import json
import time

from mmi2grpc import IUT
import socket

# Define color codes
COLOR_GREEN = '\033[32m'  # Green
COLOR_RED = '\033[31m'  # Red
COLOR_YELLOW = '\033[33m'  # Yellow
COLOR_ORANGE = '\033[38;5;208m'  # Orange

# Define result constants
RESULT_PASS = 'PASS'
RESULT_FAIL = 'FAIL'
RESULT_INCONC = 'INCONC'
RESULT_INCOMP = 'INCOMP'  # Initial final verdict meaning that test has not completed yet
RESULT_NONE = 'NONE'  # Error verdict usually indicating internal PTS error

def print_result(result):
    if result == RESULT_PASS:
        print(f"{COLOR_GREEN}{result}\033[0m")
    elif result == RESULT_FAIL:
        print(f"{COLOR_RED}{result}\033[0m")
    elif result == RESULT_INCONC or result == RESULT_INCOMP:
        print(f"{COLOR_YELLOW}{result}\033[0m")
    elif result == RESULT_NONE:
        print(f"{COLOR_ORANGE}{result}\033[0m")
    else:
        print("Invalid result value")


def run_automatically(test_name):
    dut = IUT(test_name, [])
    pts_address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
    print(f"Test case :{test_name}")
    while True:
        # print("Waiting data from client!")
        data_from_client = client_socket.recv(1024).decode()
        if "Test_is_done" in data_from_client:
            result = data_from_client.split(':')[1]
            print_result(result)
            break
        elif len(data_from_client) >= 3:
            # print("Received from client:", data_from_client)
            mmid, description_data = data_from_client.split('::')

            out = dut.interact(pts_address, profile, test_name, MMI_ID[mmid], description_data, "")
            print(out)
            client_socket.send(out.encode())  # print('=============================================')
    print('!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!')


PTS_ADDRESS = "00:1B:DC:F4:B1:61"
profile = "GATT"
with open('mmi.json', 'r') as json_file:
    data = json.load(json_file)

MMI_ID = data[profile]

tests_name = ["GATT/CL/GAC/BV-01-C", "GATT/CL/GAR/BI-03-C", "GATT/CL/GAR/BI-04-C"]
IP_ADDRESS = "172.18.198.43"
PORT = 1234

print("Starting Tests")
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

try:
    server_socket.bind((IP_ADDRESS, PORT))
    server_socket.listen(5)
    client_socket, addr = server_socket.accept()
    print("Got connection from", addr)

    # Send tests name to windows
    client_socket.send("::".join(tests_name).encode())
    for test in tests_name:
        run_automatically(test)
        time.sleep(2)
    client_socket.close()
finally:
    server_socket.close()
