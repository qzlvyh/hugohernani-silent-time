package ia.hugo.silencetime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.namespace.QName;

import model.Sugestao;
import model.TempoAgendamentoSugestao;
import persistencia.SugestaoDao;
import persistencia.TASDao;
import services.AlarmesService;
import utils.ViewPagerAdapter;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;
import fragments.OpcoesPadraoFragment;
import fragments.OpcoesPadraoFragment.OpcoesPadraoFragmentInterface;
import fragments.SugestoesFragment;
import fragments.SugestoesFragment.SugestoesFragmentInterface;

public class SilenciarOpcoes extends FragmentActivity 
		implements OnPageChangeListener, OnTabChangeListener,
					SugestoesFragmentInterface,
					OpcoesPadraoFragmentInterface{
	
	private static final String IS_BY_SUGESTAO = "is_by_SUGESTAO";

	private SharedPreferences prefs;
	private Editor editor;
	
	private static Integer posTab;
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	private HashMap<String, TabInfo> mapTabInfo = new
			HashMap<String, SilenciarOpcoes.TabInfo>();
	List<Fragment> fragments;

	// Objeto armazenado na aba.
	private class TabInfo{
		private String tag;
		private Class<?> classe;
		private Bundle args;
		private Fragment fragment;
		
		TabInfo(String tag, Class<?> classe, Bundle args) {
			this.tag = tag;
			this.classe = classe;
			this.args = args;
		}
	}
	
	// to create a view to tab
	class TabFactory implements TabContentFactory{
		
		private final Context mContext;
		
		public TabFactory(Context context){
			this.mContext = context;
		}
		
		@Override
		public View createTabContent(String tag){
			View v = new View(mContext);
			v.setMinimumHeight(0);
			v.setMinimumWidth(0);
			return v;
		}
	}

	private Intent mIntentService;
	private Calendar mCalendar;
	private Calendar calendarAtual;
	
	private Integer horas;
	private Integer minutos;
	private Long diasRestantes;
	private boolean guardarComoSugestao = false;
	
	private SugestaoDao daoSugestao;
	

	private final Integer IDENTIFICADOR_PENDING_INTENT_AND_NOTIFICACAO = 432473264;

	private Notification.Builder builderNotification;
	private NotificationManager notificationManager;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_silenciar_opcoes);
		
		prefs = getSharedPreferences(AlarmesService.NAME_PREFERENCES_APPLICATION, Context.MODE_PRIVATE);
		editor = prefs.edit();
		
		daoSugestao = new SugestaoDao(this);
		
		mIntentService = new Intent(this, AlarmesService.class);
		
		mCalendar = new GregorianCalendar();
		calendarAtual = new GregorianCalendar();
		
		// inicializando o hor�rio
		this.horas = calendarAtual.get(Calendar.HOUR_OF_DAY);
		this.minutos = calendarAtual.get(Calendar.MINUTE);
		
		this.initializeTabHost(savedInstanceState);
		if (savedInstanceState != null){
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tag"));
		}
		this.initializeViewPager();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				TASDao daoTas = new TASDao(getApplicationContext());
				
				List<TempoAgendamentoSugestao> lista = daoTas.getMelhoresSugestoes();
				
				final int tamanho = lista.size();
				for (int i = 0; i < tamanho; i++) {
					if (lista.size() < 2){
						break;
					}else{
						lista.remove(new Random().nextInt(lista.size()));
					}
				}
				
				for (TempoAgendamentoSugestao tas : lista) {
					PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PARTIAL_WAKE_LOCK");
			        //Acquire the lock
			        wl.acquire();
					
					notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					Intent i = new Intent(getApplicationContext(), ListaSugestoesAgendamentoActivity.class);
					
					i.putExtra(ListaSugestoesAgendamentoActivity.ID_TAS, tas.getId());
					
					PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
							IDENTIFICADOR_PENDING_INTENT_AND_NOTIFICACAO,
							i, PendingIntent.FLAG_UPDATE_CURRENT);
					
					builderNotification = new Notification.Builder(getApplicationContext())
							.setContentTitle("Sugest�o " + tas.getTitulo())
							.setContentText("Clique para ver esta sugest�o e outras")
							.setSmallIcon(R.drawable.aprendendo)
							.setContentIntent(pi)
							.setDefaults(Notification.DEFAULT_ALL)
							.setWhen(System.currentTimeMillis())
							.setAutoCancel(true);
							
					notificationManager.notify(IDENTIFICADOR_PENDING_INTENT_AND_NOTIFICACAO,
							builderNotification.getNotification());
					builderNotification.setDefaults(Notification.DEFAULT_LIGHTS);
				}
				
				for (TempoAgendamentoSugestao tempoAgendamentoSugestao : lista) {
					daoTas.decrementarPeso(tempoAgendamentoSugestao);
				}
				
			}
		}).start();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("tag", mTabHost.getCurrentTabTag());
		super.onSaveInstanceState(outState);
	}
	
	private void initializeViewPager(){
		
		fragments = new ArrayList<Fragment>();
		fragments.add(Fragment.instantiate(this, SugestoesFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, OpcoesPadraoFragment.class.getName()));
		this.mPagerAdapter = new ViewPagerAdapter(super.getSupportFragmentManager(), fragments);
		this.mViewPager = (ViewPager)
				findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
	}
	
	private void initializeTabHost(Bundle args){
		mTabHost = (TabHost)
				findViewById(android.R.id.tabhost);
		mTabHost.setup();
		TabInfo tabInfo = null;
		SilenciarOpcoes.AddTab(this, this.mTabHost,
				this.mTabHost.newTabSpec("Tab1").setIndicator("Sugest�es"),
				(tabInfo = new TabInfo("Tab1", SugestoesFragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		
		SilenciarOpcoes.AddTab(this, this.mTabHost,
				this.mTabHost.newTabSpec("Tab2").setIndicator("Op��es Padr�o"),
				(tabInfo = new TabInfo("Tab2", OpcoesPadraoFragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		mTabHost.setOnTabChangedListener(this);
	}
	
	private static void AddTab(SilenciarOpcoes activity,
			TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo){
		
		// attach a tab view factory to spec
		tabSpec.setContent(activity.new TabFactory(activity));
		tabHost.addTab(tabSpec);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.silenciar_opcoes, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.itSilenciar:
				if (diasRestantes != null){
					mCalendar.set(Calendar.HOUR_OF_DAY, horas);
					mCalendar.set(Calendar.MINUTE, minutos);
					
					if (guardarComoSugestao){
						Sugestao sugestao = new Sugestao(diasRestantes.intValue());
						daoSugestao.open();
						daoSugestao.create(sugestao);
						daoSugestao.close();
					}
					
					editor.putLong(AlarmesService.CALENDAR_TIME_IN_MILLIS, mCalendar.getTimeInMillis());
					editor.commit();

					startService(mIntentService);
					
//					SilenceTimeWidgetProvider.onUpdateMessage(this.getApplicationContext(), diasRestantes.intValue(), AlarmesService.AGENDAMENTOS_CRIADOS);
					finish();
				}else{
					Toast.makeText(this,
							"Verifique se voc� selecionou " +
							"o per�odo corretamente",
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.itListarControlesDeSom:
				Intent intent = new Intent(SilenciarOpcoes.this, ListaAgendamentosActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
				break;
			case R.id.itZerarSugestoes:
				daoSugestao.apagarTudo();
				recreate();
				break;
			case R.id.itListarTAS:
				Intent i = new Intent(getApplicationContext(), ListaSugestoesAgendamentoActivity.class);
				startActivity(i);
				overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
				
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public String setDiasRestantes(Calendar mCalendar) {
		
		final long umDiaEmMillesegundos = 86400000;
		final long tempoSelecionado = mCalendar.getTimeInMillis();
		final long tempoAtual = calendarAtual.getTimeInMillis();
		
		diasRestantes = (tempoSelecionado - tempoAtual) / umDiaEmMillesegundos;
		
		return String.valueOf(diasRestantes);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		this.mTabHost.setCurrentTab(position);
	}

	@Override
	public void onTabChanged(String tag) {
		posTab = this.mTabHost.getCurrentTab();
		this.mViewPager.setCurrentItem(posTab);
		
		if (tag == "Tab2"){
			
			try {
				Thread.sleep(100);
				
				View view = getCurrentFocus();
				
				TextView tvDataEscolhida = (TextView) view.findViewById(R.id.tvDataEscolhida);
				TextView tvDiasRestantes = (TextView) view.findViewById(R.id.tvDiasRestantes);
				
				if (prefs.getBoolean(IS_BY_SUGESTAO, false)){

					String data = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)) + "/"
							+ String.valueOf(mCalendar.get(Calendar.MONTH)+1) + "/"
							+ String.valueOf(mCalendar.get(Calendar.YEAR));

					tvDataEscolhida.setText(data);
					tvDiasRestantes.setText(
							setDiasRestantes(mCalendar));
				}

			} catch (InterruptedException e) {
				Toast.makeText(this, "N�o foi poss�vel capturar as informa��es da sugest�o", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			
		}
		
	}

	@Override
	public void setTempoDeEspera(Integer tempoDeEspera) {
		mCalendar = new GregorianCalendar();
		mCalendar.add(Calendar.DAY_OF_MONTH, tempoDeEspera);
		editor.putBoolean(IS_BY_SUGESTAO, true);
		editor.commit();
	}

	@Override
	public void setHorarioEscolhido(int hour, int minute) {
		this.horas = hour;
		this.minutos = minute;
	}

	@Override
	public void changeTab(String tag) {	
		mTabHost.setCurrentTabByTag(tag);
	}

	@Override
	public void salvarSugestao(boolean salvarSugestao) {
		this.guardarComoSugestao = salvarSugestao;
	}}