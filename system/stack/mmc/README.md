# Minijailed Media Codec (MMC) for ChromeOS BT
To reduce the privilge of audio codec in BT daemon, MMC uses minijail to launch
and sandbox external codec libraries.

## Apply MMC to New Codec
### 1. Implement codec server
  * Wraps third party library codes.
  * Codec server should inherit MMC Interface.
    * public methods: `init`, `cleanup`, `transcode`.
    * `init`: set up transcoder and return frame size accepted by the transcoder.
    * `cleanup`: clear the transcoder context.
    * `transcode`: transcode input data, store result in the given output buffer,
                   and return the transcoded data length.
### 2. Add codec proto message in mmc_config.proto
  * Define a proto message for a codec, may include:
    * Init configuration.
    * Transcode arguments or params.
    * Third party library constant/enum mapping.
  * Add message field in `ConfigParam`.
### 3. Add codec support in MMC daemon
  * Add codec server creation in `CodecInit`.
### 4. Add codec client in BT process
  * BT process accesses library via codec client
    * `init`: set up ConfigParam and pass it to codec client
    * `transcode`: pass input and output buffer and specify the input data size
                   and the output buffer capacity. the transcode return value
                   will be the output data length on success, and negative error
                   number otherwise.
    * `cleanup`: when a session ends, cleanup should be called.

## Related link
* Design doc: go/floss-mmc
* Presentation: go/floss-mmc-presentation
* Performance experiment: go/floss-mmc-experiment
