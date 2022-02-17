package com.zy.androidcrop.ext

import android.app.Activity
import android.widget.Toast

/**
 * @Author: fzy
 * @Date: 2022/2/17
 * @Description:
 */

fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
