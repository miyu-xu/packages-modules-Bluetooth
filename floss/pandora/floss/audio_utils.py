from floss.pandora.floss import cras_utils
from floss.pandora.floss import utils
import logging
import os

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
def get_selected_output_device_type():
    """Gets the selected audio output node type.

    @returns: The node type of the selected output device.
    """
    # Note: should convert the dbus.String to the regular string.
    return str(cras_utils.get_selected_output_device_type())


@utils.dbus_safe(None)
def select_output_node(node_type):
    """Selects the audio output node.

    @param node_type: The node type of the Bluetooth peer device.

    @returns: True if the operation succeeds.
    """
    return cras_utils.set_single_selected_output_node(node_type)


def select_audio_output_node():
    """Select the audio output node through cras."""

    def bluetooth_type_selected(node_type):
        """Check if the bluetooth node type is selected."""
        selected = get_selected_output_device_type()
        logging.debug('active output node type: %s, expected %s',
                      selected, node_type)
        print(selected)
        return selected == node_type

    node_type = CRAS_BLUETOOTH_OUTPUT_NODE_TYPE
    if not select_output_node(node_type):
        raise RuntimeError('select_audio_output_node failed')

    desc = 'waiting for %s as active cras audio output node type' % node_type
    _poll_for_condition(lambda: bluetooth_type_selected(node_type),
                             desc=desc)
