#
# Copyright 2021 Google, Inc.
#

TOOLS_DIR = tools


elc3_src += \
    $(TOOLS_DIR)/elc3.c \
    $(TOOLS_DIR)/lc3bin.c \
    $(TOOLS_DIR)/wave.c

elc3_lib += liblc3
elc3_ldlibs += m
elc3_ldflags += -flto

$(eval $(call add-bin,elc3))


dlc3_src += \
    $(TOOLS_DIR)/dlc3.c \
    $(TOOLS_DIR)/lc3bin.c \
    $(TOOLS_DIR)/wave.c

dlc3_lib += liblc3
dlc3_ldlibs += m
elc3_ldflags += -flto

$(eval $(call add-bin,dlc3))


.PHONY: tools
tools: elc3 dlc3
