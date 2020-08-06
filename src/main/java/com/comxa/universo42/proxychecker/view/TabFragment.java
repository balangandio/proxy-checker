package com.comxa.universo42.proxychecker.view;

import android.content.ClipData;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.comxa.universo42.proxychecker.R;

import java.util.ArrayList;

abstract public class TabFragment extends android.support.v4.app.Fragment {
    private ListView lista;
    private ArrayList<String> proxies;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment_sshs, container, false);

        LinearLayout ll = (LinearLayout) view.findViewById(R.id.linearLayoutTab);

        lista = new ListView(getContext());
        lista.setOnItemClickListener(getOnItemClickLista());

        ll.addView(lista);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        proxies = getProxies();

        lista.setAdapter(new ArrayAdapter<String>(getContext(), R.layout.my_simple_list_item_1, proxies));
    }

    abstract public ArrayList<String> getProxies();

    public ProxyListActivity getProxyActivity() {
        return (ProxyListActivity) getActivity();
    }

    public AdapterView.OnItemClickListener getOnItemClickLista() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicaoLinhaSelecionada, long id) {
                setClipBoardStr(proxies.get(posicaoLinhaSelecionada));
                Toast.makeText(getActivity(), getString(R.string.msgCopiado), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setClipBoardStr(String str) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setText(str);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newPlainText("simple text", str));
        }
    }


    public static class TabProxies extends TabFragment{
        @Override
        public ArrayList<String> getProxies() {
            return getProxyActivity().getProxies();
        }
    }

    public static class TabProxyOns extends TabFragment {
        @Override
        public ArrayList<String> getProxies() {
            return getProxyActivity().getProxyOns();
        }
    }

    public static class TabProxyOffs extends TabFragment {
        @Override
        public ArrayList<String> getProxies() {
            return getProxyActivity().getProxyOffs();
        }
    }

    public static class TabProxyEmpty extends TabFragment {
        @Override
        public ArrayList<String> getProxies() {
            return new ArrayList<String>();
        }
    }
}
