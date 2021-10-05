package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.res.AssetManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import net.harimurti.tv.R
import net.harimurti.tv.adapter.CountryAdapter
import net.harimurti.tv.adapter.OnCountryClickedListener
import net.harimurti.tv.adapter.OnProxyClickedListener
import net.harimurti.tv.adapter.ProxyAdapter
import net.harimurti.tv.databinding.ProxyDialogBinding
import net.harimurti.tv.extension.setFullScreenFlags
import net.harimurti.tv.extra.Preferences
import net.harimurti.tv.extra.ProxyReader
import net.harimurti.tv.model.ProxyData
import net.harimurti.tv.model.ProxyList
import net.harimurti.tv.model.ProxySource


class ProxyDialog : DialogFragment(), OnCountryClickedListener, OnProxyClickedListener {
    private var _binding : ProxyDialogBinding? = null
    private val binding get() = _binding!!
    private var countryAdapter: CountryAdapter = CountryAdapter(country)
    private lateinit var proxySources: ProxySource
    private var proxyAdapter: ProxyAdapter = ProxyAdapter(proxies)

    companion object {
        private var proxies: ArrayList<ProxyData> = ArrayList()
        var country: ArrayList<ProxySource.Country> = ArrayList()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            //full screen
            dialog.window!!.setLayout(width, height)
            dialog.window?.setFullScreenFlags()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.ProxyDialogThemeOverlay)
        dialog.setCanceledOnTouchOutside(false)

        proxySources = Gson().fromJson(
            requireContext().assets.readAssetsFile("proxy_list.json"), ProxySource::class.java)

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ProxyDialogBinding.inflate(inflater,container, false)
        val dialogView = binding.root

        setAdapter()

        //button Refresh
        binding.proxyRefresh.setOnClickListener {
            refreshProxy()
        }

        //button close
        binding.proxyClose.setOnClickListener {
            dismiss()
        }

        return dialogView
    }

    fun setAdapter(){

        var result = proxySources.country.filter(fun(m: ProxySource.Country): Boolean {
            return proxies.any { m.code.equals(it.country) }
        }) as ArrayList<ProxySource.Country>
        if(Preferences().proxies != null) {
            proxies = Preferences().proxies!!
            //result.clear()
            result = proxySources.country.filter(fun(m: ProxySource.Country): Boolean {
                return proxies.any { m.code.equals(it.country) }
            }) as ArrayList<ProxySource.Country>
        }
        country.addAll(result)

        //recycler view
        proxyAdapter = ProxyAdapter(proxies)
        binding.proxyAdapter = proxyAdapter
        proxyAdapter.filter.filter("")
        binding.proxyList.layoutManager = GridLayoutManager(context, spanColumn())
        proxyAdapter.setOnProxyClickedListener(this)

        countryAdapter = CountryAdapter(country)
        binding.countryAdapter = countryAdapter
        countryAdapter.setOnShareClickedListener(this)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun spanColumn(): Int {
        val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        return ((screenWidthDp * 0.75) / 300 + 0.5).toInt()
    }

    private fun refreshProxy(){
        binding.proxyAdapter?.clear()
        binding.countryAdapter?.clear()
        val proxyListSet = ProxyList()

        ProxyReader().set(proxySources.proxysource, object: ProxyReader.Result {
            override fun onError(source: String, error: String) {
                val snackbar = Snackbar.make(binding.root, "[${error.uppercase()}] $source", Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction(android.R.string.ok) { snackbar.dismiss() }
                snackbar.show()
            }

            override fun onResponse(proxyList: ProxyList?) {
                if (proxyList != null) proxyListSet.mergeWith(proxyList)
                else Toast.makeText(context, R.string.playlist_cant_be_parsed, Toast.LENGTH_SHORT).show()
            }

            override fun onFinish() {
                proxies.clear()
                proxies.addAll(proxyListSet.proxies)
                Preferences().proxies = proxies

                setAdapter()
            }
        }).process()
    }

    fun ProxyList?.mergeWith(proxyList: ProxyList?) {
        if (proxyList == null) return
        proxyList.proxies.let { this?.proxies?.addAll(it) }
    }

    private fun AssetManager.readAssetsFile(fileName : String): String =
        open(fileName).bufferedReader().use{it.readText()}

    override fun onCountryClicked(countryCode: String?) {
        proxyAdapter.filter.filter(countryCode)
    }

    override fun onProxyClicked() {
        dismiss()
    }

}
