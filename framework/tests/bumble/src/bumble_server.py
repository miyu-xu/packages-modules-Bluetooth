from apps import pandora_server
from bumble import pandora as bumble_server
from bumble_experimental.dck import DckService
from pandora_experimental.dck_grpc_aio import add_DckServicer_to_server
import logging

if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_DckServicer_to_server(DckService(bumble.device), server))
    pandora_server.main()  # pylint: disable=no-value-for-parameter
