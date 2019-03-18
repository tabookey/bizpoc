package com.tabookey.bizpoc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.tabookey.bizpoc.api.TokenInfo


class CryptoCurrencySpinnerAdapter(context: Context, items: List<TokenInfo>)
    : ArrayAdapter<TokenInfo>(context, resource, items) {

    companion object {
        const val resource = R.layout.row_cryptocurrencies_alert_dialog
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView ?: {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(resource, null)
        }()
        return bindView(v, position)
    }

    private fun bindView(v: View, position: Int): View {
        val tv = v.findViewById(R.id.coin_name) as TextView
        val iv = v.findViewById(R.id.coin_logo) as ImageView
        val currency = getItem(position) ?: return v
        Glide.with(context).load(currency.logo).into(iv)
        tv.text = currency.name
        return v
    }

}