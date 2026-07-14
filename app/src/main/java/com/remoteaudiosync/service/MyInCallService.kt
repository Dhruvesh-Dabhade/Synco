package com.remoteaudiosync.service

import android.telecom.Call
import android.telecom.InCallService

class MyInCallService : InCallService() {
    companion object {
        var activeCall: Call? = null
        var listener: CallListener? = null
    }

    interface CallListener {
        fun onCallAdded(call: Call)
        fun onCallRemoved(call: Call)
        fun onCallStateChanged(call: Call, state: Int)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            listener?.onCallStateChanged(call, state)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        call.registerCallback(callCallback)
        listener?.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (activeCall == call) {
            activeCall = null
        }
        listener?.onCallRemoved(call)
    }
}
