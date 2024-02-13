import json
from mmi2grpc import IUT


def get_test_data(test_name, data_file):
    # Load the JSON data
    with open(data_file, "r") as f:
        data = json.load(f)

    for item in data:
        if item["test_name"] == test_name:
            inter = item["interactions"]
            desc = item["descriptions"]
            break  # results = {  #   "Interactions": item["interactions"],  #   "Descriptions": item["descriptions"]  # }
    return inter, desc


def run_test():
    dut = IUT(test_name, [])
    pts_address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
    dut.modem = Modem(grpc.insecure_channel(f"localhost:{dut.pandora_server_port}"))
    for i in range(0, len(interaction)):
        print(f"mmi step name: {interaction[i]}\n mmi description: { description[i]}")
        user_input = input(
            f'Enter action required (press enter/s/m/exit).')
        action = interaction[i] if i < len(interaction) else ""
        test_description = description[i]

        if user_input == 's':
            continue
        elif user_input == 'exit':
            break
        elif user_input == 'm':
            des_user_input = input(f'Enter desc: .')
            test_description = des_user_input

        else:
            pass
        out = dut.interact(pts_address, profile, test_name, action, test_description, "")
        print(f"Output:{out}")
        print("============================================================")


DATA_FILE = "gatt_data.json"
PTS_ADDRESS = "00:1B:DC:F4:B1:61"
profile = "GATT"
test_name = "GATT/SR/GAW/BV-08-C"
interaction, description = get_test_data(test_name,DATA_FILE)
run_test()
