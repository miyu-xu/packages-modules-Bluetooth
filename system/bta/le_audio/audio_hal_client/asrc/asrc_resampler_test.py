#!/usr/bin/env python3

import ctypes
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
from scipy import signal
import sys

matplotlib.use('QtAgg')

class CResampler:

    def __init__(self, lib, channels, bitdepth):

        self.lib = lib
        self.channels = channels
        self.bitdepth = bitdepth

    def resample(self, xs, ratio):

        c_int = ctypes.c_int
        c_size_t = ctypes.c_size_t
        c_double = ctypes.c_double
        c_int16_p = ctypes.POINTER(ctypes.c_int16)
        c_int32_p = ctypes.POINTER(ctypes.c_int32)

        channels = self.channels
        bitdepth = self.bitdepth

        xs_min = -(2**(bitdepth-1))
        xs_max =  (2**(bitdepth-1) - 1)
        xs_int = np.rint(np.clip(np.ldexp(xs, bitdepth-1), xs_min, xs_max)).\
                 astype([np.int16, np.int32][bitdepth > 16], 'C')

        ys_int = np.empty(int(np.ceil(len(xs) / ratio)), dtype=xs_int.dtype)

        if bitdepth <= 16:
            lib.resample_i16(
              c_int(channels), c_int(bitdepth), c_double(ratio),
              xs_int.ctypes.data_as(c_int16_p), c_size_t(len(xs_int)),
              ys_int.ctypes.data_as(c_int16_p), c_size_t(len(ys_int)))
        else:
            lib.resample_i32(
              c_int(channels), c_int(bitdepth), c_double(ratio),
              xs_int.ctypes.data_as(c_int32_p), c_size_t(len(xs_int)),
              ys_int.ctypes.data_as(c_int32_p), c_size_t(len(ys_int)))

        return np.ldexp(ys_int, 1-bitdepth)

FS = 48e3

def snr(x, fs=FS):

  f, p = signal.periodogram(x,
          fs=fs, scaling='spectrum', window=('kaiser', 38))

  k = np.argmax(p)
  s = np.sum(p[k-19:k+20])
  n = np.sum(p[20:k-19]) + np.sum(p[k+20:])

  return 10*np.log10(s/n)

lib = ctypes.cdll.LoadLibrary("asrc_resampler.so")
cresampler_16 = CResampler(lib, 1, 16)
cresampler_24 = CResampler(lib, 1, 24)

N  = 8192
xt = np.arange(2*N+128) / FS

f_snr = []
y16u_snr = []
y24u_snr = []
y16d_snr = []
y24d_snr = []

ratio = 48.0 / 44.1

for f in range(200, 20000, 99):
    xs = np.sin(2 * np.pi * xt * f)

    f_snr += [ f ]

    y16 = cresampler_16.resample(xs, 44.1 / 48.0)
    y24 = cresampler_24.resample(xs, 44.1 / 48.0)

    y16u_snr = y16u_snr + [ snr(y16[128:128+N]) ]
    y24u_snr = y24u_snr + [ snr(y24[128:128+N]) ]

    y16 = cresampler_16.resample(xs, 48.0 / 44.1)
    y24 = cresampler_24.resample(xs, 48.0 / 44.1)

    y16d_snr = y16d_snr + [ snr(y16[128:128+N]) ]
    y24d_snr = y24d_snr + [ snr(y24[128:128+N]) ]


k = np.argmin(np.abs(np.array(f_snr) - 18e3))
y16u_snr_mean = np.mean(y16u_snr[:k])
y16d_snr_mean = np.mean(y16d_snr[:k])
y24u_snr_mean = np.mean(y24u_snr[:k])
y24d_snr_mean = np.mean(y24d_snr[:k])

if False:
    plt.plot(f_snr, y16u_snr, label='44.1 -> 48 KHz, 16 bits ({:4.1f} dB)'.format(y16u_snr_mean))
    plt.plot(f_snr, y16d_snr, label='48 -> 44.1 KHz, 16 bits ({:4.1f} dB)'.format(y16d_snr_mean))
    plt.plot(f_snr, y24u_snr, label='44.1 -> 48 KHz, 24 bits ({:4.1f} dB)'.format(y24u_snr_mean))
    plt.plot(f_snr, y24d_snr, label='48 -> 44.1 KHz, 24 bits ({:4.1f} dB)'.format(y24d_snr_mean))
    plt.legend()
    plt.show()

sys.exit(y16u_snr_mean >  84 and y16d_snr_mean >  84 and \
         y24u_snr_mean > 112 and y24d_snr_mean > 112     )
