package com.daozhao.hello.ui.dashboard

import android.R.attr.button
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.daozhao.hello.R
import com.daozhao.hello.databinding.FragmentDashboardBinding
import com.google.firebase.installations.FirebaseInstallations
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsProfile
import okhttp3.*
import java.io.IOException


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var mContext: Context? = null

    private var root: View? = null

    companion object {
        private const val TAG: String = "DashboardFragment"
        private const val GET_AAID = 1
        private const val DELETE_AAID = 2
        private const val CODELABS_ACTION: String = "com.daozhao.push.action"
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val dashboardViewModel =
                ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        root = binding.root


        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root as ConstraintLayout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity!!.findViewById<Button>(R.id.getToken).setOnClickListener { v ->
            when (v?.id) {
                R.id.getToken -> {
                    Log.i(TAG, "clickId")
                    getToken()
                }
            }
        }
//

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showLog(log: String?) {
        activity?.runOnUiThread {
            val textView = root!!.findViewById<TextView?>(R.id.text_dashboard)
            textView.text = log
        }
    }

    private fun getToken() {
        showLog("getToken:begin")
        object : Thread() {
            override fun run() {
                try {
                    // read from agconnect-services.json
                    val appId = "xxx"

                    var instance = HmsInstanceId.getInstance(mContext);
                    val token = instance.getToken(appId, "HCM")
                    storeTokenProfile(token)
                } catch (e: ApiException) {
                    Log.e(TAG, "get token failed, $e")
                    showLog("get token failed, $e")
                }
            }
        }.start()
    }

    private fun storeTokenProfile(token: String?) {
        Log.i(TAG, "get token:$token")
        showLog("get token:$token")
        if (!TextUtils.isEmpty(token)) {
            // 将token存储在自己的服务器，方便后期推送
            sendRegTokenToServer(token);
            // 添加当前设备上的用户与应用的关系。
            val hmsProfileInstance: HmsProfile = HmsProfile.getInstance(mContext);
            if (hmsProfileInstance.isSupportProfile) {
                hmsProfileInstance.addProfile(HmsProfile.CUSTOM_PROFILE, "9105385871708200535")
                    .addOnCompleteListener { task ->
                        // 获取结果
                        if (task.isSuccessful){
                            Log.i(TAG, "add profile success.")
                        } else{
                            Log.e(TAG, "add profile failed: " + task.exception.message)
                        }
                    }
            }
        }
    }

    private fun sendRegTokenToServer(token: String?) {
        Log.i(TAG, "sending token to server. token:$token")
        // 借助Firebase获取一个唯一的id作为设备标识
        FirebaseInstallations.getInstance().id.addOnCompleteListener { it ->
            run {
                if (it.isComplete) {
                    var uuid = it.result.toString();
                    showLog("uuid complete " + uuid);
//                    val paramMap: HashMap<String, Any> = HashMap()
//                    paramMap["id"] = uuid;
//                    paramMap["pushToken"] = token.toString();
//                    val result = HttpUtil.post("https://www.baidu.com", paramMap);

                    val client = OkHttpClient.Builder().build()

                    var formBody: FormBody.Builder = FormBody.Builder();
                    formBody.add("id", uuid);
                    formBody.add("pushToken", token.toString());
                    val request: Request = Request.Builder()
                        .url("https://gateway.daozhao.com.cn/HMS/storePushToken")
                        .post(formBody.build())
                        .build()
                    val call: okhttp3.Call = client.newCall(request)
                    call.enqueue(object : Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            showLog("fetch failed " + uuid)
                            Log.e(TAG, "fetch failed " + uuid);
                        }

                        override fun onResponse(call: okhttp3.Call, response: Response) {
                            val result: String = response.body!!.string()
                            showLog("fetch success " + uuid)
                            Log.i(TAG, "fetch success " + uuid);
                        }
                    })
                }
            }
        }
    }
}