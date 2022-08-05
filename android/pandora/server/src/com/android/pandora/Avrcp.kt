package com.android.pandora

import android.content.Context
import pandora.AVRCPGrpc.AVRCPImplBase
import pandora.AvrcpProto.*

class Avrcp(val context: Context) : AVRCPImplBase() {
  private val TAG = "PandoraAvrcp"

}
