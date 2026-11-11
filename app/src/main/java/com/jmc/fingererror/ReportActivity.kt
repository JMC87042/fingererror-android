package com.jmc.fingererror

import android.app.Activity
import android.os.Bundle
import android.view.View

/** 앱의 내 엄지 리포트 화면 (내용은 ReportPanel — 키보드 안 리포트와 같은 모양) */
class ReportActivity : Activity() {
    private lateinit var panel: ReportPanel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.statusBarColor = android.graphics.Color.rgb(242, 242, 247)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        panel = ReportPanel(this, false, { Learner(this).lastReport() }, onReset = { Learner(this).requestReset() })
        setContentView(panel.view)
        panel.render()
    }

    override fun onResume() {
        super.onResume()
        panel.render()
    }
}
