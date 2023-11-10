
#include <iostream>

#define LOG(n) std::cout
#include "../audio_source_hal_asrc.cc"

namespace bluetooth::hal {
void NocpIsoClocker::Register(NocpIsoHandler *) {}
void NocpIsoClocker::Unregister() {}
}

namespace le_audio {

class LeAudioSourceAudioHalAsrcTest : public LeAudioSourceAudioHalAsrc {

 public:
  LeAudioSourceAudioHalAsrcTest(int channels, int bitdepth) :
    LeAudioSourceAudioHalAsrc(channels, 48000, bitdepth, 10000) {}

  void ResampleI16(double ratio,
      const int16_t* in, size_t in_length, size_t *in_count,
      int16_t* out, size_t out_length, size_t *out_count) {

    auto channels = (*resampler_i16_).size();
    unsigned sub_q26;

    for (auto& r: *resampler_i16_)
      r.Resample(round(ldexp(ratio, 26)),
          in , channels, in_length  / channels, in_count,
          out, channels, out_length / channels, out_count, &sub_q26);
  }

  void ResampleI32(double ratio,
      const int32_t* in, size_t in_length, size_t *in_count,
      int32_t* out, size_t out_length, size_t *out_count) {

    auto channels = (*resampler_i32_).size();
    unsigned sub_q26;

    for (auto& r: *resampler_i32_)
      r.Resample(round(ldexp(ratio, 26)),
          in , channels, in_length  / channels, in_count,
          out, channels, out_length / channels, out_count, &sub_q26);
  }
};

extern "C" void resample_i16(int channels, int bitdepth, double ratio,
    const int16_t *in, size_t in_length, int16_t *out, size_t out_length) {

  size_t in_count, out_count;

  LeAudioSourceAudioHalAsrcTest(channels, bitdepth).
     ResampleI16(ratio, in, in_length, &in_count, out, out_length, &out_count);

  if (out_count < out_length)
    printf("wrong output size: %zd:%zd %zd:%zd\n", in_length, in_count, out_length, out_count);

  return;
}

extern "C" void resample_i32(int channels, int bitdepth, double ratio,
    const int32_t *in, size_t in_length, int32_t *out, size_t out_length) {

  size_t in_count, out_count;

  LeAudioSourceAudioHalAsrcTest(channels, bitdepth).
     ResampleI32(ratio, in, in_length, &in_count, out, out_length, &out_count);

  if (out_count < out_length)
    printf("wrong output size: %zd:%zd %zd:%zd\n", in_length, in_count, out_length, out_count);

  return;
}

} // namespace le_audio
