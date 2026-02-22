package com.aslua

import android.util.Log
import java.lang.Exception

/**
 * 用于显示错误信息
 */

class TextWarriorException(msg: String?) : Exception(msg) {
    companion object {
        private const val NDEBUG = false

        @JvmStatic
		fun fail(details: String) {
            assertVerbose(false, details)
        }

        @JvmStatic
		fun assertVerbose(condition: Boolean, details: String) {
            if (NDEBUG) {
                return
            }

            if (!condition) {
                System.err.print("TextWarrior assertion failed: ")
                System.err.println(details)
                Log.i("lua", details)
            }
        }
    }
}
