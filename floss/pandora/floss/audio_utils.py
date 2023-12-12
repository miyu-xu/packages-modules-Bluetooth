from floss.pandora.floss import cras_utils
from floss.pandora.floss import utils
import logging
import os

# CRAS_BLUETOOTH_INPUT_NODE_TYPE = 'BLUETOOTH'
CRAS_BLUETOOTH_INPUT_NODE_TYPE = 'FRONT_MIC'
CRAS_BLUETOOTH_OUTPUT_NODE_TYPE = 'BLUETOOTH'

DATA_DIR = '/tmp'
AUDIO_TEST_DIR = '/usr/local/autotest/cros/audio/test_data'
AUDIO_RECORD_DIR = '/tmp/audio'

a2dp_test_data = {
    'rate': 48000,
    'channels': 2,
    'frequencies': (440, 20000),
    'file': os.path.join(AUDIO_TEST_DIR,
                         'binaural_sine_440hz_20000hz_rate48000_5secs.wav'),
    'recorded_by_sink': os.path.join(AUDIO_RECORD_DIR,
                                     'a2dp_recorded_by_sink.wav'),
    'chunk_in_secs': 5,
    'bit_width': 16,
    'format': 'S16_LE',
    'duration': 5,
}


def _poll_for_condition(condition, timeout=20, sleep_interval=1,
                        desc='waiting for condition'):
    """Polls until a condition is evaluated to true.

    @param condition: Function taking no args and returning anything that will evaluate to True in a conditional check.
    @param timeout: Maximum number of seconds to wait.
    @param sleep_interval: Time to sleep between polls.
    @param desc: Description of default TimeoutError used if 'exception' is None.

    @returns: True on success. False otherwise.
    """
    try:
        utils.poll_for_condition(condition=condition,
                                 timeout=timeout,
                                 sleep_interval=sleep_interval,
                                 desc=desc)
    except Exception as e:
        logging.error('Exception occurred when %s (%s)' % (desc, e))
        return False

    return True


@utils.dbus_safe(None)
def get_selected_input_device_type():
    """Gets the selected audio input node type.

    @returns: The node type of the selected input device.
    """
    # Note: should convert the dbus.String to the regular string.
    print('get_selected_input_device_type')
    return str(cras_utils.get_selected_input_device_type())


@utils.dbus_safe(None)
def get_selected_output_device_type():
    """Gets the selected audio output node type.

    @returns: The node type of the selected output device.
    """
    # Note: should convert the dbus.String to the regular string.
    print('get_selected_output_device_type')
    return str(cras_utils.get_selected_output_device_type())

@utils.dbus_safe(None)
def select_input_node(node_type):
    """Selects the audio input node.

    @param node_type: The node type of the Bluetooth peer device.

    @returns: True if the operation succeeds.
    """
    print('select_input_node')
    return cras_utils.set_single_selected_input_node(node_type)


@utils.dbus_safe(None)
def select_output_node(node_type):
    """Selects the audio output node.

    @param node_type: The node type of the Bluetooth peer device.

    @returns: True if the operation succeeds.
    """
    print('select_output_node')
    return cras_utils.set_single_selected_output_node(node_type)


def _test_select_audio_input_node(node_type=None):
    """Selects the audio input node through cras.

    @param node_type: A str representing node type defined in CRAS_NODE_TYPES.
    @raises: error.TestError if failed.

    @returns: True if select given node success.
    """
    print('_test_select_audio_input_node')

    def node_type_selected(node_type):
        """Checks if the given node type is selected."""
        selected = get_selected_input_device_type()
        print(f'selected: {selected}')
        logging.debug('active input node type: %s, expected %s', selected,
                      node_type)
        return selected == node_type

    desc = 'waiting for select_input_node'
    select_node = _poll_for_condition(
        lambda: select_input_node(node_type),
        desc=desc)

    desc = 'waiting for %s as active cras audio input node type' % node_type
    logging.debug(desc)
    node_selected = _poll_for_condition(lambda: node_type_selected(node_type), desc=desc)
    print(select_node, node_selected)
    return select_node and node_selected


def _test_select_audio_output_node(node_type=None):
    """Selects the audio output node through cras.

    @param node_type: A str representing node type defined in CRAS_NODE_TYPES.
    @raises: error.TestError if failed.

    @returns: True if select given node success.
    """
    print('_test_select_audio_output_node')

    def node_type_selected(node_type):
        """Checks if the given node type is selected."""
        selected = get_selected_output_device_type()
        print(f'selected: {selected}')
        logging.debug('active output node type: %s, expected %s', selected,
                      node_type)
        return selected == node_type

    desc = 'waiting for select_output_node'
    select_node = _poll_for_condition(
        lambda: select_output_node(node_type),
        desc=desc)

    desc = 'waiting for %s as active cras audio output node type' % node_type
    logging.debug(desc)
    node_selected = _poll_for_condition(lambda: node_type_selected(node_type), desc=desc)
    print(select_node, node_selected)
    return select_node and node_selected


def test_select_audio_input_node_bluetooth():
    """Selects the Bluetooth device as input node.

    @returns: True on success. False otherwise.
    """
    return _test_select_audio_input_node(CRAS_BLUETOOTH_INPUT_NODE_TYPE)

def test_select_audio_output_node_bluetooth():
    """Selects the Bluetooth device as output node.

    @returns: True on success. False otherwise.
    """
    return _test_select_audio_output_node(CRAS_BLUETOOTH_OUTPUT_NODE_TYPE)
