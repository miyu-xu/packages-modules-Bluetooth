#pragma once
#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <ostream>
#include <new>

using HANDLE_LHDC_BT = void*;
using __LHDC_ENC_TYPE__ = unsigned int;
using LHDC_ENC_TYPE_T = __LHDC_ENC_TYPE__;

struct _lhdc_abr_para_t {
  uint32_t version;
  uint32_t sample_rate;
  uint32_t bits_per_sample;
  uint32_t bits_per_sample_ui;
  uint32_t upBitrateCnt;
  uint32_t upBitrateSum;
  uint32_t dnBitrateCnt;
  uint32_t dnBitrateSum;
  uint32_t lastBitrate;
  uint32_t qualityStatus;
};

using lhdc_abr_para_t = _lhdc_abr_para_t;

using __LHDC_SAMPLE_FREQ__ = unsigned int;
using __LHDC_QUALITY__ = unsigned int;
using __LHDC_ABR_TYPE__ = unsigned int;
using __LHDC_FUNC_RET__ = int;

constexpr static const __LHDC_SAMPLE_FREQ__ LHDC_SR_192000HZ = 192000;
constexpr static const __LHDC_SAMPLE_FREQ__ LHDC_SR_96000HZ = 96000;
constexpr static const __LHDC_SAMPLE_FREQ__ LHDC_SR_48000HZ = 48000;
constexpr static const __LHDC_SAMPLE_FREQ__ LHDC_SR_44100HZ = 44100;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_INVALID = 130;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_CTRL_END = 129;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_CTRL_RESET_ABR = 128;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_UNLIMIT = 14;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_AUTO = 13;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_MAX_BITRATE = 12;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH5 = 12;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH4 = 11;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH3 = 10;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH2 = 9;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH1 = 8;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_HIGH = 7;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_MID = 6;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW = 5;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW4 = 4;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW3 = 3;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW2 = 2;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW1 = 1;
constexpr static const __LHDC_QUALITY__ LHDC_QUALITY_LOW0 = 0;
constexpr static const __LHDC_ENC_TYPE__ LHDC_ENC_TYPE_INVALID = 2;
constexpr static const __LHDC_ENC_TYPE__ LHDC_ENC_TYPE_LHDC = 1;
constexpr static const __LHDC_ENC_TYPE__ LHDC_ENC_TYPE_UNKNOWN = 0;
constexpr static const __LHDC_ABR_TYPE__ LHDC_ABR_INVALID = 4;
constexpr static const __LHDC_ABR_TYPE__ LHDC_ABR_192K_RES = 3;
constexpr static const __LHDC_ABR_TYPE__ LHDC_ABR_96K_RES = 2;
constexpr static const __LHDC_ABR_TYPE__ LHDC_ABR_48K_RES = 1;
constexpr static const __LHDC_ABR_TYPE__ LHDC_ABR_44K_RES = 0;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_BUF_NOT_ENOUGH = -11;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_ERROR = -10;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_AR_NOT_READY = -9;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_CODEC_NOT_READY = -8;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_CODEC = -7;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_HANDLE_AR = -6;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_HANDLE_CBUF = -5;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_HANDLE_ENC = -4;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_HANDLE_PARA = -3;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_HANDLE_CB = -2;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_INVALID_INPUT_PARAM = -1;
constexpr static const __LHDC_FUNC_RET__ LHDC_FRET_SUCCESS = 0;

extern "C" {
extern uint32_t gABR_table_index;
extern uint32_t auto_bitrate_adjust_table_lhdc_44k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_48k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_96k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_192k[6];

int32_t lhdcBT_autoBR_reset_abr_index();
int32_t lhdcBT_autoBR_adjust_bitrate_process(HANDLE_LHDC_BT handle, uint32_t queue_len);
int32_t lhdcBT_autoBR_adjust_bitrate_init(HANDLE_LHDC_BT handle);
}  // extern "C"

struct nbyte_program_struct {
  unsigned int *enc_w_mem;
  unsigned char *enc_r_begin;
  unsigned char *enc_r_finsih;
  unsigned int enc_r_d32_l;
  unsigned int enc_r_num;
};
struct arith_mdata_struct {
  unsigned char mdata_para_0[128];
  int mdata_para_1;
  int mdata_para_2;
  int mdata_para_3;
};
struct arith_sdata_struct {
  unsigned int sdata_para_0;
  unsigned int sdata_para_1[6];
  unsigned int sdata_para_2[5];
  unsigned int sdata_para_3[5];
};
struct enc_arith_struct {
  unsigned int enc_arth_para_0;
  unsigned int enc_arth_para_1;
  unsigned char *enc_arth_para_2;
  unsigned char *enc_arth_para_3;
  unsigned char *enc_arth_para_4;
  arith_mdata_struct enc_arth_para_5;
  arith_sdata_struct enc_arth_para_6;
};
struct segment_setting_struct {
  float drity_bit_adding;
  int segment_num_inv;
  int segment_scale_jump;
  int segment_scale_level;
  int codeing_step_0;
  int segment_num;
  int arith_init_0;
  int arith_init_1;
  int segment_offset[34];
  int segment_scale[34];
};
struct segment_cutoff_struct {
  int segment_size;
  int segment_cuteoff;
};
struct kiss_fft_cpx {
  int r;
  int i;
};
struct kiss_fft_state {
  int nfft;
  int inverse;
  int factors[64];
  kiss_fft_cpx *twiddles;
};
using kiss_fft_cfg = kiss_fft_state*;
struct frequncy_buffer_struct {
  int *freq_data;
  int *fft_iv;
  kiss_fft_cpx *fft_coef_in;
  kiss_fft_cpx *fft_coef_out;
  void *bpc_buf_s_rd_padding;
  void *bpc_buf_s_rb_padding;
  void *bpc_buf_s_bna_padding;
  void *bpc_buf_s_q_padding;
  void *td_s_dat_padding;
  void *td_s_fdat_padding;
  void *td_s_rd_padding;
  void *td_s_uc_padding;
};
using lhdc_enc_workspace_content_index = unsigned int;
struct hdr_s {
  unsigned short info;
  unsigned char ext_data[10];
  int enc_frm_len_provided;
  unsigned char enc_frm_len_need_udpate;
  int meta_data_loop_count;
};
using print_lhdc_log_fp = void(*)(char*);
using lhdc_log_level = unsigned int;
struct lhdc_enc_cirbuf_s {
  unsigned int idx;
  unsigned int odx;
  unsigned int s_len;
  unsigned int r_len;
  unsigned int max_len;
  unsigned int item_cnt;
  unsigned char *cbuf;
};
using lhdc_enc_circbuf_t = lhdc_enc_cirbuf_s;
struct _lhdc_para_t {
  uint32_t version;
  uint32_t sample_rate;
  uint32_t bits_per_sample;
  uint32_t bits_per_sample_ui;
  uint32_t upBitrateCnt;
  uint32_t upBitrateSum;
  uint32_t dnBitrateCnt;
  uint32_t dnBitrateSum;
  uint32_t lastBitrate;
  uint32_t qualityStatus;
  uint32_t actual_bitrate;
  uint32_t max_bitrate_inx;
  uint32_t min_bitrate_inx;
  uint32_t samples_per_frame;
  uint32_t frame_duration;
  uint32_t max_frame_per_packet;
  uint32_t frame_per_packet;
  uint32_t host_mtu_size;
  uint32_t target_mtu_size;
  uint32_t encode_interval;
  uint32_t encoded_frame_size;
  uint32_t max_frame_per_interval;
  bool updateFramneInfo;
  void *lhdc_enc;
  uint8_t *cirbuff;
  lhdc_enc_circbuf_t input_cbuf;
  uint32_t enc_in_buf_bytes;
  uint8_t *enc_in_buf;
  uint32_t enc_out_buf_bytes;
  uint8_t *enc_out_buf;
};

using lhdc_para_t = _lhdc_para_t;
using __LHDC_SAMPLE_FREQ__ = unsigned int;
using __LHDCBT_SMPL_FMT__ = unsigned int;
using __LHDC_FRAME_DURATION__ = unsigned int;
using __LHDC_ENC_INTERVAL__ = unsigned int;
using __LHDC_QUALITY__ = unsigned int;
using __LHDC_MTU_SIZE__ = unsigned int;
using __LHDC_VERSION__ = unsigned int;
using __LHDC_ABR_TYPE__ = unsigned int;
using __LHDC_LOG_LEVEL__ = unsigned int;
using __LHDC_FUNC_RET__ = int;
using segment_setting_table_index_special_case_enum = unsigned int;
using hdr_info_index = unsigned int;
using lhdc_enc_error = int;
using C2RustUnnamed_0 = unsigned int;
using lhdc_enc_workspace_mode_options = unsigned int;
using __LHDC_ENC_AUX_PARAM__ = unsigned int;
using __LHDC_ENC_IN_SAMPLE_FREQ__ = unsigned int;
using __LHDC_ENC_IN_SMPL_FMT__ = unsigned int;
using __LHDC_ENC_IN_SAMPLE_FRAME__ = unsigned int;
using __LHDC_ENC_IN_FRAME_DURATION__ = unsigned int;
using __LHDC_ENC_IN_INTERVAL__ = unsigned int;
using __LHDC_ENC_IN_QUALITY__ = unsigned int;
using __LHDC_ENC_IN_MTU_SIZE__ = unsigned int;
using __LHDC_ENC_IN_VERSION__ = unsigned int;
using __LHDC_ENC_IN_FUNC_RET__ = int;

constexpr static const __LHDCBT_SMPL_FMT__ LHDCBT_SMPL_FMT_S32 = 32;
constexpr static const __LHDCBT_SMPL_FMT__ LHDCBT_SMPL_FMT_S24 = 24;
constexpr static const __LHDCBT_SMPL_FMT__ LHDCBT_SMPL_FMT_S16 = 16;
constexpr static const __LHDC_FRAME_DURATION__ LHDC_FRAME_1S = 10000;
constexpr static const __LHDC_FRAME_DURATION__ LHDC_FRAME_10MS = 100;
constexpr static const __LHDC_FRAME_DURATION__ LHDC_FRAME_7P5MS = 75;
constexpr static const __LHDC_FRAME_DURATION__ LHDC_FRAME_5MS = 50;
constexpr static const __LHDC_ENC_INTERVAL__ LHDC_ENC_INTERVAL_20MS = 20;
constexpr static const __LHDC_ENC_INTERVAL__ LHDC_ENC_INTERVAL_10MS = 10;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_MAX = 8192;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_MHDT_8DH5 = 2820;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_MHDT_6DH5 = 2089;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_MHDT_4DH5 = 1392;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_3MBPS = 1023;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_2MBPS = 660;
constexpr static const __LHDC_MTU_SIZE__ LHDC_MTU_MIN = 300;
constexpr static const __LHDC_VERSION__ LHDC_VERSION_INVALID = 2;
constexpr static const __LHDC_VERSION__ LHDC_VERSION_1 = 1;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_DEBUG = 7;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_INFO = 6;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_NOTICE = 5;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_WARNING = 4;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_ERROR = 3;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_CRIT = 2;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_ALERT = 1;
constexpr static const __LHDC_LOG_LEVEL__ LHDC_LOG_LEVEL_EMERG = 0;

constexpr static const segment_setting_table_index_special_case_enum SEGMENT_480_LB = 482;
constexpr static const segment_setting_table_index_special_case_enum SEGMENT_480 = 481;
constexpr static const segment_setting_table_index_special_case_enum SEGMENT_480_HR = 480;
constexpr static const hdr_info_index ALL_HEADER_INFO_NUM = 6;
constexpr static const hdr_info_index META_INDEX = 5;
constexpr static const hdr_info_index LARC_INDEX = 4;
constexpr static const hdr_info_index AR_INDEX = 3;
constexpr static const hdr_info_index JAS_INDEX = 2;
constexpr static const hdr_info_index VERSION_INDEX = 1;
constexpr static const hdr_info_index ENC_SIZE_INDEX = 0;
constexpr static const lhdc_enc_workspace_content_index ALL_CONTENT_NUM = 39;
constexpr static const lhdc_enc_workspace_content_index LPC_WINDOW_INDEX = 38;
constexpr static const lhdc_enc_workspace_content_index FREQ_2WR_INDEX = 37;
constexpr static const lhdc_enc_workspace_content_index FREQ_2W_INDEX = 36;
constexpr static const lhdc_enc_workspace_content_index FFT_TWIDDLE_INDEX = 35;
constexpr static const lhdc_enc_workspace_content_index FREQ_WINDOW_INDEX = 34;
constexpr static const lhdc_enc_workspace_content_index FREQ_POS_TWID_INDEX = 33;
constexpr static const lhdc_enc_workspace_content_index FREQ_PRE_TWID_INDEX = 32;
constexpr static const lhdc_enc_workspace_content_index ENCODED_DATA_CH1_INDEX = 31;
constexpr static const lhdc_enc_workspace_content_index ENCODED_DATA_CH0_INDEX = 30;
constexpr static const lhdc_enc_workspace_content_index OUTPUTTER_INDEX = 29;
constexpr static const lhdc_enc_workspace_content_index ENC_ARITH_S_INDEX = 28;
constexpr static const lhdc_enc_workspace_content_index FREQ_CH_REPEATING_INDEX = 27;
constexpr static const lhdc_enc_workspace_content_index FREQ_CH_INDEX = 26;
constexpr static const lhdc_enc_workspace_content_index EBUFFER_FREQ_HEAP_INDEX = 25;
constexpr static const lhdc_enc_workspace_content_index TD_S_UMEM8_INDEX = 24;
constexpr static const lhdc_enc_workspace_content_index TD_S_QUANT_INDEX = 23;
constexpr static const lhdc_enc_workspace_content_index TD_S_FDATA_INDEX = 22;
constexpr static const lhdc_enc_workspace_content_index TD_S_DATA_INDEX = 21;
constexpr static const lhdc_enc_workspace_content_index LEVEL_MEM_S_QUANT_2_SYMBOL_INDEX = 20;
constexpr static const lhdc_enc_workspace_content_index LEVEL_MEM_S_BIT_NUM_INDEX = 19;
constexpr static const lhdc_enc_workspace_content_index LEVEL_MEM_S_QUANT_REM_INDEX = 18;
constexpr static const lhdc_enc_workspace_content_index LEVEL_MEM_S_QUANT_INDEX = 17;
constexpr static const lhdc_enc_workspace_content_index FREQ_MEM_S_FFT_COEF_OUT_INDEX = 16;
constexpr static const lhdc_enc_workspace_content_index FREQ_MEM_S_FFT_COEF_IN_INDEX = 15;
constexpr static const lhdc_enc_workspace_content_index FREQ_MEM_S_FFT_IV_INDEX = 14;
constexpr static const lhdc_enc_workspace_content_index FREQ_MEM_S_DATA_INDEX = 13;
constexpr static const lhdc_enc_workspace_content_index EBUFFER_ENCDATA_FDATA_INDEX = 12;
constexpr static const lhdc_enc_workspace_content_index EBUFFER_CHD1_TDATA_FOR_OV_INDEX = 11;
constexpr static const lhdc_enc_workspace_content_index EBUFFER_CHD0_TDATA_FOR_OV_INDEX = 10;
constexpr static const lhdc_enc_workspace_content_index EBUFFER_INDEX = 9;
constexpr static const lhdc_enc_workspace_content_index HDR_S_INDEX = 8;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MAX_TD_UMEM8_LEN_INDEX = 7;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MAX_TD_FRAME_SIZE_INDEX = 6;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MAX_FREQ_REPEATING_INDEX = 5;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MAX_LOSSY_UMEM8_LEN_INDEX = 4;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MAX_FRAME_SIZE_INDEX = 3;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MODE_INDEX = 2;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_MS_MAX_INDEX = 1;
constexpr static const lhdc_enc_workspace_content_index PARAMETER_KHZ_MAX_INDEX = 0;
constexpr static const lhdc_enc_error LHDC_ENC_ERROR = -1;
constexpr static const lhdc_enc_error LHDC_ENC_OK = 0;
constexpr static const C2RustUnnamed_0 OFF = 2;
constexpr static const C2RustUnnamed_0 SCALE = 1;
constexpr static const C2RustUnnamed_0 PARM = 0;
constexpr static const lhdc_enc_workspace_mode_options LHDC_ENC_MODE_OPTION_0 = 0;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_DEBUG_NO_LOG = 256;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_MAX = 135;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_DEBUG_INTERNAL = 128;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_DEBUG = 7;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_INFO = 6;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_NOTICE = 5;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_WARNING = 4;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_ERROR = 3;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_CRIT = 2;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_ALERT = 1;
constexpr static const lhdc_log_level LHDC_LOGMGR_LEVEL_EMERG = 0;
constexpr static const __LHDC_ENC_AUX_PARAM__ LHDC_LE_2M_MAX_ENC_FRAME_BYTES = 1255;
constexpr static const __LHDC_ENC_AUX_PARAM__ LHDC_MAX_QUEUE_FRAMES = 4;
constexpr static const __LHDC_ENC_AUX_PARAM__ LHDC_MAX_ENC_FRAME_SAMPLE = 960;
constexpr static const __LHDC_ENC_IN_SAMPLE_FREQ__ LHDC_ENC_IN_SR_192000HZ = 192000;
constexpr static const __LHDC_ENC_IN_SAMPLE_FREQ__ LHDC_ENC_IN_SR_96000HZ = 96000;
constexpr static const __LHDC_ENC_IN_SAMPLE_FREQ__ LHDC_ENC_IN_SR_48000HZ = 48000;
constexpr static const __LHDC_ENC_IN_SAMPLE_FREQ__ LHDC_ENC_IN_SR_44100HZ = 44100;
constexpr static const __LHDC_ENC_IN_SMPL_FMT__ LHDC_ENC_IN_SMPL_FMT_S32 = 32;
constexpr static const __LHDC_ENC_IN_SMPL_FMT__ LHDC_ENC_IN_SMPL_FMT_S24 = 24;
constexpr static const __LHDC_ENC_IN_SMPL_FMT__ LHDC_ENC_IN_SMPL_FMT_S16 = 16;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_MAX_SAMPLE_FRAME = 1920;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_10MS_192000KHZ = 1920;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_10MS_96000KHZ = 960;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_10MS_48000KHZ = 480;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_10MS_44100KHZ = 480;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_5MS_192000KHZ = 960;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_5MS_96000KHZ = 480;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_5MS_48000KHZ = 240;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_5MS_44100KHZ = 240;
constexpr static const __LHDC_ENC_IN_SAMPLE_FRAME__ LHDC_ENC_IN_SAMPLE_FRAME_2P5MS_96000KHZ = 240;
constexpr static const __LHDC_ENC_IN_FRAME_DURATION__ LHDC_ENC_IN_FRAME_1S = 10000;
constexpr static const __LHDC_ENC_IN_FRAME_DURATION__ LHDC_ENC_IN_FRAME_10MS = 100;
constexpr static const __LHDC_ENC_IN_FRAME_DURATION__ LHDC_ENC_IN_FRAME_7P5MS = 75;
constexpr static const __LHDC_ENC_IN_FRAME_DURATION__ LHDC_ENC_IN_FRAME_5MS = 50;
constexpr static const __LHDC_ENC_IN_INTERVAL__ LHDC_ENC_IN_INTERVAL_20MS = 20;
constexpr static const __LHDC_ENC_IN_INTERVAL__ LHDC_ENC_IN_INTERVAL_10MS = 10;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_INVALID = 130;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_CTRL_END = 129;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_CTRL_RESET_ABR = 128;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_UNLIMIT = 14;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_AUTO = 13;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_MAX_BITRATE = 12;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH5 = 12;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH4 = 11;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH3 = 10;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH2 = 9;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH1 = 8;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_HIGH = 7;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_MID = 6;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW = 5;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW4 = 4;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW3 = 3;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW2 = 2;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW1 = 1;
constexpr static const __LHDC_ENC_IN_QUALITY__ LHDC_ENC_IN_QUALITY_LOW0 = 0;
constexpr static const __LHDC_ENC_IN_MTU_SIZE__ LHDC_ENC_IN_MTU_MAX = 8192;
constexpr static const __LHDC_ENC_IN_MTU_SIZE__ LHDC_ENC_IN_MTU_3MBPS = 1023;
constexpr static const __LHDC_ENC_IN_MTU_SIZE__ LHDC_ENC_IN_MTU_2MBPS = 660;
constexpr static const __LHDC_ENC_IN_MTU_SIZE__ LHDC_ENC_IN_MTU_MIN = 300;
constexpr static const __LHDC_ENC_IN_VERSION__ LHDC_ENC_IN_VERSION_INVALID = 2;
constexpr static const __LHDC_ENC_IN_VERSION__ LHDC_ENC_IN_VERSION_1 = 1;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_BUF_NOT_ENOUGH = -11;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_ERROR = -10;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_AR_NOT_READY = -9;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_CODEC_NOT_READY = -8;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_CODEC = -7;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_HANDLE_AR = -6;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_HANDLE_CBUF = -5;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_HANDLE_ENC = -4;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_HANDLE_PARA = -3;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_HANDLE_CB = -2;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_INVALID_INPUT_PARAM = -1;
constexpr static const __LHDC_ENC_IN_FUNC_RET__ LHDC_ENC_IN_FRET_SUCCESS = 0;

extern "C" {
extern char lhdc_tester_strLog[512];
extern segment_cutoff_struct segment_finish[18];
extern print_lhdc_log_fp lhdc_enc_log_fp;
extern char *lhdc_enc_log_strLog;
extern int lhdc_enc_log_debug_level;
extern int32_t g_bitrate_table_44k[15];
extern int32_t g_bitrate_table_48k[15];
extern int32_t g_bitrate_table_96k[15];
extern int32_t g_bitrate_table_192k[15];
extern int32_t g_bitrate_table[15];
extern size_t g_bitrate_table_size;
extern size_t g_bitrate_table_192k_size;
extern size_t g_bitrate_table_96k_size;
extern size_t g_bitrate_table_48k_size;
extern size_t g_bitrate_table_44k_size;
extern uint32_t gABR_table_index;
extern uint32_t auto_bitrate_adjust_table_lhdc_44k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_48k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_96k[6];
extern uint32_t auto_bitrate_adjust_table_lhdc_192k[6];

float lhdc_power_of_2_mode_2(float x);
float lhdc_sqrt_of_x_mode_1(float x);
void lhdc_enc_arith_start(enc_arith_struct *enc_arith, unsigned char *out, int size);
void lhdc_enc_arith_symbol_num_program(enc_arith_struct *enc_arith,
                                       const unsigned int *count,
                                       int symbol_num);
int lhdc_enc_arith_symbol_1(enc_arith_struct *enc_arith, unsigned int symbol);
void lhdc_enc_arith_symbol_2(enc_arith_struct *enc_arith, unsigned int symbol);
void lhdc_enc_arith_program(enc_arith_struct *enc_arith,
                            const unsigned int *count,
                            unsigned int symbol_num,
                            int shifting_num);
int lhdc_enc_arith_symbol_3(enc_arith_struct *enc_arith, unsigned int symbol);
int lhdc_enc_arith_clear(enc_arith_struct *enc_arith, nbyte_program_struct *bit_outputter);
int lhdc_enc_arith_esti_1(unsigned char *udata8,
                          int size,
                          const unsigned int *counting_table,
                          unsigned int sumbol_num);
int lhdc_enc_arith_esti_2(unsigned char *udata8,
                          int size,
                          const unsigned int *counting_table,
                          unsigned int sumbol_num,
                          int mean_size);
void lhdc_nbyte_program_1(nbyte_program_struct *bit_outputter,
                          unsigned int *umem32,
                          unsigned int size);
int lhdc_nbyte_program_3(nbyte_program_struct *bit_outputter, unsigned char *data, int length);
int lhdc_nbyte_program_clear(nbyte_program_struct *bit_outputter);
void lhdc_shift_by_offset_2(segment_setting_struct *segment, int *input, int *output);
const unsigned int *lhdc_table_init_fq_table(int init_fq_table_index);
const int *lhdc_table_power_of_2_table();
const segment_cutoff_struct *lhdc_table_segment_cutoff();
const int *lhdc_table_segment_configuration(int lhdc_table_segment_config_table_index,
                                            int feature_table_index);
float gmax_calc(float a, float b, float c, int d, int len, int e);
float offset_calc(int offset_idx, int start, float offset_jump);
void lhdc_enc_level_table_init(int size,
                               int resolution,
                               int hz,
                               int ms,
                               float *offset_tbl,
                               int *start,
                               float *offset_jump);
size_t lhdc_fft_get_size();
kiss_fft_cfg lhdc_fft_alloc(int nfft,
                            int inverse_fft,
                            void *mem,
                            size_t *lenmem,
                            kiss_fft_cpx *ptr_fft_twid);
void lhdc_fft_stride(kiss_fft_cfg st, const kiss_fft_cpx *fin, kiss_fft_cpx *fout, int in_stride);
int lhdc_enc_lossy_frequency_size_read();
int lhdc_enc_lossy_frequency_table_start(void *heap_memory,
                                         int frequency_size,
                                         int repeating,
                                         int sft_num,
                                         frequncy_buffer_struct *frequency_buf,
                                         kiss_fft_cpx *p_pre_twid,
                                         kiss_fft_cpx *p_pos_twid,
                                         int *ptr_mdct_win,
                                         kiss_fft_cpx *p_pos_twiddle);
void lhdc_enc_lossy_frequency_clear_space(void *heap_memory);
void lhdc_enc_lossy_frequency_operation(void *heap_memory, int c, int *input, int *output);
void lhdc_enc_lossy_frequency_overlap(int repeating,
                                      int *win_hann,
                                      float *freq_2w,
                                      float *freq_2wr);
int enc_process_header(hdr_s *hdr, int ch, int *enc_frm_len_usable, unsigned char *encoded_frame);
int lhdc_enc_get_encoded_frame_size(int *encoded_frame_size, void *heap_memory);
int lhdc_enc_get_encoded_frame_size_hdr_per_ch(int *encoded_frame_size, void *heap_memory);
int lhdc_enc_set_version(int version, void *heap_memory);
int lhdc_enc_set_jas_flag(int jas_flag, void *heap_memory);
int lhdc_enc_set_ar_flag(int ar_flag, void *heap_memory);
int lhdc_enc_set_larc_flag(int larc_flag, void *heap_memory);
int lhdc_enc_set_meta_flag_and_data(int meta_flag,
                                    unsigned char *meta_data,
                                    int loop_count,
                                    void *heap_memory);
int lhdc_enc_get_version(int *version, void *heap_memory);
int lhdc_enc_get_jas_flag(int *jas_flag, void *heap_memory);
int lhdc_enc_get_ar_flag(int *ar_flag, void *heap_memory);
int lhdc_enc_get_larc_flag(int *larc_flag, void *heap_memory);
int lhdc_enc_get_meta_flag_and_data(int *meta_flag, unsigned char *meta_data, void *heap_memory);
int lhdc_enc_freq_shift(segment_setting_struct *segment,
                        int cutoff,
                        int *data,
                        int *data_sft,
                        unsigned char *jump,
                        int *first_idx,
                        unsigned int *logarithm_by_2_tbl_0);
void lhdc_freq_shift_apply_encode(segment_setting_struct *segment,
                                  int *power_of_2_tbl,
                                  int *data_sft,
                                  int *data,
                                  int size);
int lhdc_enc_lossy_start(void *heap_memory,
                         int heap_memory_size,
                         int ch_num,
                         int resolution,
                         int khz,
                         int ms,
                         int enc_pixel_size_byte);
int lhdc_enc_lossy_frame_length_read(void *heap_memory);
unsigned int lhdc_enc_lossy_frame_number_read(void *heap_memory);
int lhdc_enc_lossy_frame_length_program(int enc_pixel_size_byte, void *heap_memory);
int lhdc_enc_lossy_frame_size_program(void *heap_memory);
void lhdc_enc_top(int *data_in,
                  unsigned int *data_L_out,
                  int *data_L_out_size,
                  unsigned int *data_R_out,
                  int *data_R_out_size,
                  void *heap_memory);
int lhdc_enc_init(int channel, int resolution, int khz, int ms, int bitrate, void *heap_memory);
int lhdc_enc_init_with_nbytes_2_ch(int channel,
                                   int resolution,
                                   int khz,
                                   int ms,
                                   int frame_len_2x,
                                   void *heap_memory);
int lhdc_enc_init_with_nbytes(int channel,
                              int resolution,
                              int khz,
                              int ms,
                              int nbytes,
                              void *heap_memory);
int lhdc_enc_get_samples_per_frame(int *s_fps, void *heap_memory);
int lhdc_enc_set_bitrate(int bitrate, void *heap_memory);
int lhdc_enc_set_nbytes(int cdata_size, void *heap_memory);
int lhdc_enc_encode(int *input_buffer_top,
                    unsigned char *output_buffer_top,
                    int *output_frame_size,
                    void *heap_memory);
int lhdc_enc_encode_hdr_per_ch(int *input_buffer_top,
                               unsigned char *output_buffer_top,
                               int *output_frame_size,
                               void *heap_memory);
void *lhdc_enc_get_mem_content_addr(void *heap_memory, lhdc_enc_workspace_content_index index);
int lhdc_enc_workspace_get_size(int ch, int khz_max, int ms_max, int mode, int *size);
int lhdc_enc_workspace_init(int ch, int khz_max, int ms_max, int mode, void *heap_memory);
int lhdc_enc_util_register_log_cb(print_lhdc_log_fp cb, char *mgr_buffer, lhdc_log_level level);
int lhdc_enc_util_log_level_set(lhdc_log_level level);
int32_t lhdc_util_enc_register_log_cb(void *cb, char *mgr_lic_buff, int32_t level);
int32_t lhdc_util_get_lib_auth_string(uint8_t *str_buf, uint32_t buf_byte_size);
int32_t lhdc_util_free_handle(HANDLE_LHDC_BT handle);
int32_t lhdc_util_get_mem_req(uint32_t version, uint32_t *mem_req_bytes);
int32_t lhdc_util_get_handle(uint32_t version, HANDLE_LHDC_BT handle, uint32_t mem_size);
int32_t lhdc_util_get_target_bitrate(HANDLE_LHDC_BT handle, uint32_t *bitrate);
int32_t lhdc_util_set_target_bitrate_inx(HANDLE_LHDC_BT handle,
                                         uint32_t bitrate_inx,
                                         uint32_t *bitrate_inx_set,
                                         bool upd_qual_status);
int32_t lhdc_util_get_current_mtu(HANDLE_LHDC_BT handle, uint32_t *current_mtu);
int32_t lhdc_util_set_target_mtu(HANDLE_LHDC_BT handle, uint32_t target_mtu);
int32_t lhdc_util_set_max_bitrate_inx(HANDLE_LHDC_BT handle,
                                      uint32_t max_bitrate_inx,
                                      uint32_t *max_bitrate_inx_set);
int32_t lhdc_util_set_min_bitrate_inx(HANDLE_LHDC_BT handle,
                                      uint32_t min_bitrate_inx,
                                      uint32_t *min_bitrate_inx_set);
int32_t lhdc_util_adjust_bitrate(HANDLE_LHDC_BT handle,
                                 LHDC_ENC_TYPE_T *enc_type_ptr,
                                 lhdc_abr_para_t **abr_para_ptr);
int32_t lhdc_util_reset_up_bitrate(HANDLE_LHDC_BT handle);
int32_t lhdc_util_reset_down_bitrate(HANDLE_LHDC_BT handle);
int32_t lhdc_util_init_encoder(HANDLE_LHDC_BT handle,
                               uint32_t sampling_freq,
                               uint32_t bits_per_sample,
                               uint32_t bitrate_inx,
                               uint32_t frame_duration,
                               uint32_t mtu,
                               uint32_t interval);
int32_t lhdc_util_get_block_Size(HANDLE_LHDC_BT handle, uint32_t *block_size);
int32_t lhdc_util_enc_process(HANDLE_LHDC_BT handle,
                              void *p_pcm,
                              uint32_t pcm_bytes,
                              uint8_t *out_put,
                              uint32_t out_buf_bytes,
                              uint32_t *written,
                              uint32_t *out_frames);
int32_t lhdc_util_get_bitrate(uint32_t bitrate_inx, uint32_t *bitrate);
int32_t lhdc_util_get_bitrate_inx(uint32_t bitrate, uint32_t *bitrate_inx);
int32_t lhdc_encoder_get_mem_req(uint32_t version, uint32_t *mem_req_bytes);
int32_t lhdc_encoder_new(uint32_t version, lhdc_para_t *handle, uint32_t mem_size);
int32_t lhdc_encoder_init(lhdc_para_t *lhdc,
                          uint32_t sampling_freq,
                          uint32_t bits_per_sample,
                          uint32_t bitrate_inx,
                          uint32_t frame_duration,
                          uint32_t mtu,
                          uint32_t interval,
                          uint32_t hdr_per_channel);
int32_t lhdc_encoder_resource_reset(lhdc_para_t *lhdc);
int32_t lhdc_encoder_get_target_bitrate(lhdc_para_t *lhdc, uint32_t *bitrate);
int32_t lhdc_encoder_get_bitrate(uint32_t bitrate_inx, uint32_t *bitrate);
int32_t lhdc_encoder_get_bitrate_inx(uint32_t bitrate, uint32_t *bitrate_inx);
int32_t lhdc_encoder_set_target_bitrate_inx(lhdc_para_t *lhdc, uint32_t bitrate_inx);
int32_t lhdc_encoder_set_target_mtu(lhdc_para_t *lhdc, uint32_t target_mtu);
int32_t lhdc_encoder_set_max_bitrate_inx(lhdc_para_t *lhdc, uint32_t max_bitrate_inx);
int32_t lhdc_encoder_set_min_bitrate_inx(lhdc_para_t *lhdc, uint32_t min_bitrate_inx);
int32_t lhdc_encoder_get_frame_len(lhdc_para_t *lhdc, uint32_t *samples_per_frame);
int32_t lhdc_encoder_encode(lhdc_para_t *lhdc,
                            uint8_t *in_0,
                            uint32_t in_buf_bytes,
                            uint8_t *out,
                            uint32_t out_buf_bytes,
                            uint32_t *written_bytes,
                            uint32_t *out_frames,
                            uint32_t force_encode,
                            uint32_t hdr_per_channel);
void lhdc_enc_cirbuf_init(lhdc_enc_cirbuf_s *pcb, unsigned char *buf, int len);
void lhdc_enc_cirbuf_reset(lhdc_enc_cirbuf_s *pcb);
int lhdc_enc_cirbuf_len(lhdc_enc_cirbuf_s *pcb);
int lhdc_enc_cirbuf_empty_len(lhdc_enc_cirbuf_s *pcb);
int lhdc_enc_cirbuf_get(lhdc_enc_cirbuf_s *pcb, unsigned char *buf, int len);
int lhdc_enc_cirbuf_put(lhdc_enc_cirbuf_s *pcb, unsigned char *buf, int len);
int lhdc_enc_cirbuf_put_no_copy(lhdc_enc_cirbuf_s *pcb, unsigned char **buf, int len);
int lhdc_enc_cirbuf_get_no_copy(lhdc_enc_cirbuf_s *pcb, unsigned char **buf, int len);
int32_t lhdcBT_autoBR_reset_abr_index();
int32_t lhdcBT_autoBR_adjust_bitrate_process(HANDLE_LHDC_BT handle, uint32_t queue_len);
int32_t lhdcBT_autoBR_adjust_bitrate_init(HANDLE_LHDC_BT handle);
}  // extern "C"

