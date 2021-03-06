package persistencia;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import model.Agendamento;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.util.Log;

public class EventsAndroid {
	
	/*
	 * Referente ao CalendarProvider
	 */
	
	private final String[] projectionEvents = new String[]{
			Events._ID,
			Events.DTSTART,
			Events.DTEND
			};
	

 private final String[] projectionInstances = new String[]{
			Instances._ID,
			Instances.TITLE,
			Instances.DESCRIPTION,
			Instances.BEGIN,
			Instances.END,
			Instances.EVENT_ID
	};
	
		
	private Context mContexto;
	private ContentResolver contentResolver;
	private Uri.Builder eventsUriBuilder;
	private Uri.Builder instancesUriBuilder;
	private Uri mUriEvent;
	private Uri mUriInstanceEvent;
	private AgendamentoSilenciosoDao dao;

	public EventsAndroid(Context context){
		this.mContexto = context;
		this.contentResolver = context.getContentResolver();
		dao = new AgendamentoSilenciosoDao(context);
		eventsUriBuilder = Events.CONTENT_URI.buildUpon();
		instancesUriBuilder = Instances.CONTENT_URI.buildUpon();
	}
	
	public List<Agendamento> getAllEventsOfWeek(){
		dao.open();
		List<Agendamento> lista = new ArrayList<Agendamento>();
		
		Calendar calFistDayOfWeek = new GregorianCalendar();
//		calFistDayOfWeek.set(Calendar.DAY_OF_WEEK, calFistDayOfWeek.getFirstDayOfWeek());
		
		System.out.println("Dia primeiro: " + calFistDayOfWeek.get(Calendar.DAY_OF_MONTH));
		
		Calendar calLastDayOfWeek = new GregorianCalendar();
		calLastDayOfWeek.set(Calendar.DAY_OF_WEEK, 7);
		System.out.println("Dia final: " + calLastDayOfWeek.get(Calendar.DAY_OF_MONTH));
		
		String selection = Events.DTSTART + " > ? AND " + Events.DTEND + " < ?";
		String[] selecionArgs = new String[]{String.valueOf(calFistDayOfWeek.getTimeInMillis()),
				String.valueOf(calLastDayOfWeek.getTimeInMillis())};
		
		Cursor cursor = contentResolver.query(mUriEvent, projectionEvents,
				selection, selecionArgs, Events.DTSTART + " ASC");
		

		if (cursor.moveToFirst()){
			do{
				Long id_evento = cursor.getLong(0);
				if (cursor.getInt(5) != Events.AVAILABILITY_BUSY) continue;
				Agendamento agendamento = dao.getAgendamentoByEventoId(id_evento);
				if (agendamento != null){
					lista.add(agendamento);
					continue;
				}else{
					
					agendamento = new Agendamento(
							cursor.getString(1), cursor.getString(2),
							cursor.getLong(3), cursor.getLong(4), id_evento
							);
					lista.add(agendamento);
					
					Log.i("EventoTitulo", "Titulo: " + cursor.getString(1));
				}
			}while(cursor.moveToNext());
		}
		cursor.close();
		dao.createAtCascade(lista);
		dao.close();
		return lista;
	}
	
	public List<Agendamento> getAllEventsOfQuinzena(){
		dao.open();
		List<Agendamento> lista = new ArrayList<Agendamento>();
		
		Calendar calFistDayOfWeek = new GregorianCalendar();
		Calendar calLastDayOfWeek = new GregorianCalendar();
		calLastDayOfWeek.set(Calendar.DAY_OF_MONTH, calFistDayOfWeek.get(Calendar.DAY_OF_MONTH)+15);
		
		String selection = Events.DTSTART + " > ? AND " + Events.DTEND + " < ?";
		String[] selecionArgs = new String[]{String.valueOf(calFistDayOfWeek.getTimeInMillis()),
				String.valueOf(calLastDayOfWeek.getTimeInMillis())};
		
		Cursor cursor = contentResolver.query(mUriEvent, projectionEvents,
				selection, selecionArgs, Events.DTSTART + " ASC");
		

		if (cursor.moveToFirst()){
			do{
				Long id_evento = cursor.getLong(0);
				if (cursor.getInt(5) != Events.AVAILABILITY_BUSY) continue;
				Agendamento agendamento = dao.getAgendamentoByEventoId(id_evento);
				if (agendamento != null){
					lista.add(agendamento);
					continue;
				}else{
					
					agendamento = new Agendamento(
							cursor.getString(1), cursor.getString(2),
							cursor.getLong(3), cursor.getLong(4), id_evento
							);
					lista.add(agendamento);
					
					Log.i("EventoTitulo", "Titulo: " + cursor.getString(1));
				}
			}while(cursor.moveToNext());
		}
		cursor.close();
		dao.createAtCascade(lista);
		dao.close();
		return lista;

	}

	public ArrayList<Agendamento> getEvents(long dataLimiteTimeInMillis) {
		ArrayList<Agendamento> lista = new ArrayList<Agendamento>();
		
		Long timeAtual = System.currentTimeMillis();
		
		if (dataLimiteTimeInMillis != -1L){
						
			eventsUriBuilder = Events.CONTENT_URI.buildUpon();
			mUriEvent = eventsUriBuilder.build();
			
			Log.i("EventoCaptura", "Iniciando a captura dos eventos da dataInMillis " +
					System.currentTimeMillis() +
					" at� a dataInMillis " +
					dataLimiteTimeInMillis);
			
			String selection = Events.TITLE + " like ? or " + Events.DESCRIPTION + " like ? AND "
					+ Events.DTSTART + " > ? AND " + Events.DTEND + " < ?";
			String[] selecionArgs = new String[]{
					"%reuni_o%",
					"%reuni_o%",
					String.valueOf(timeAtual),
					String.valueOf(dataLimiteTimeInMillis)
			};
			
			Cursor cursor = contentResolver.query(mUriEvent, projectionEvents,
					selection, selecionArgs, Events.DTSTART + " ASC");
			
			Log.i("EventoCursor", "Tamanho do cursor: " + cursor.getCount());

			
			cursor.moveToFirst();
			if (!cursor.isAfterLast()){
				do{
					Long id_evento = cursor.getLong(0);
					
					if (cursor.getLong(1) >= timeAtual){
						instancesUriBuilder = Instances.CONTENT_URI.buildUpon();
						ContentUris.appendId(instancesUriBuilder, Long.MIN_VALUE);
						ContentUris.appendId(instancesUriBuilder, Long.MAX_VALUE);
						mUriInstanceEvent = instancesUriBuilder.build();
						
						setInstancesAgendamentoFromIdEvent(lista, id_evento);

					}

				}while(cursor.moveToNext());
			}else{
				Log.i("EventoCaptura", "O cursor est� vazio.");
			}

			cursor.close();
		}else{
			Log.i("EventoCaptura", "N�o foi capturado uma data. O valor passada para data foi: " + dataLimiteTimeInMillis);
		}
		
		return lista;

	}

	private Integer setInstancesAgendamentoFromIdEvent(
			ArrayList<Agendamento> lista, Long id_evento) {
		
		Log.i("CursorInstance", "Iniciando a captura de inst�ncias para o evento de id " + id_evento);

		String selection = Instances.EVENT_ID + " = ?";
		String []selectionArgs = new String[]{
				String.valueOf(id_evento)
		};
		
		Cursor cursor = contentResolver.query(mUriInstanceEvent,
				projectionInstances, selection, selectionArgs,
				Instances.BEGIN + " ASC");

		Log.i("CursorInstance", "Quantidade de instancias: " + cursor.getCount());

		dao.open();
		cursor.moveToFirst();
		if (!cursor.isAfterLast()){
			do{
				long id_instancia = cursor.getLong(0);
				Agendamento agendamento = dao.getAgendamentoByIdAndIdEvent(id_instancia, id_evento);
				if (agendamento != null){
//						lista.add(agendamento);
					continue;
				}else{
					agendamento = new Agendamento();
					agendamento.setId(id_instancia);
					agendamento.setTitulo(cursor.getString(1));
					agendamento.setDescricao(cursor.getString(2));
					agendamento.setInicio(cursor.getLong(3));
					agendamento.setFim(cursor.getLong(4));
					agendamento.setId_evento(id_evento);
					lista.add(agendamento);
				}
			}while(cursor.moveToNext());

		}
		Integer criados = dao.createAtCascade(lista);
		dao.close();
		cursor.close();
		return criados;
		
	}

	public boolean saveAgendamentoByUri(Uri eventUri) {
		
		Cursor cursor = contentResolver.query(eventUri, projectionEvents,
				null, null, Events.DTSTART + " ASC");
		
		Log.i("EventoCursor", "Tamanho do cursor: " + cursor.getCount());
		
		Integer criados = 0;
		
		cursor.moveToFirst();
		if (!cursor.isAfterLast()){
			do{
				Long id_evento = cursor.getLong(0);
				
				instancesUriBuilder = Instances.CONTENT_URI.buildUpon();
				ContentUris.appendId(instancesUriBuilder, Long.MIN_VALUE);
				ContentUris.appendId(instancesUriBuilder, Long.MAX_VALUE);
				mUriInstanceEvent = instancesUriBuilder.build();

				criados += setInstancesAgendamentoFromIdEvent(new ArrayList<Agendamento>(), id_evento);
				
			}while(cursor.moveToNext());
		}else{
			Log.i("EventoCaptura", "O cursor est� vazio.");
		}
		cursor.close();
		return criados!=0?true:false;
	}

}
