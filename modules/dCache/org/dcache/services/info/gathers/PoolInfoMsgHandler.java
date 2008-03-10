package org.dcache.services.info.gathers;

import org.apache.log4j.Logger;
import org.dcache.services.info.base.*;
import dmg.util.CommandThrowableException;

public class PoolInfoMsgHandler extends CellMessageHandlerSkel {
	
	private static Logger _log = Logger.getLogger( PoolInfoMsgHandler.class);

	private StatePath _pPath = new StatePath( "pools");

	public void process(Object msgPayload, long metricLifetime) {

		StateUpdate update = new StateUpdate();
		
		if( msgPayload instanceof CommandThrowableException) {
			CommandThrowableException e = (CommandThrowableException) msgPayload;
			_log.error( "got exception from pool: ", e);
		}
		
		if( !msgPayload.getClass().isArray()) {
			_log.error( "received a message that isn't an array");
			return;
		}
			
		Object[] array = (Object []) msgPayload;
		
		if( array.length != 6) {
			_log.error( "unexpected array size: "+array.length);
			return;
		}

		Boolean isEnabled = (Boolean) array[3];
		Long heartBeat = (Long) array[4];
		Boolean isReadOnly = (Boolean) array [5];

		StatePath thisPoolPath = _pPath.newChild((String) array[0]); // pool's name
			
		addItems( update, thisPoolPath.newChild("poolgroups"), (Object []) array [1], metricLifetime); 
		addItems( update, thisPoolPath.newChild("links"), (Object []) array [2], metricLifetime); 

		update.appendUpdate( thisPoolPath.newChild("read-only"),
				new BooleanStateValue( isReadOnly.booleanValue(), metricLifetime));

		update.appendUpdate( thisPoolPath.newChild("enabled"),
				new BooleanStateValue( isEnabled.booleanValue(), metricLifetime));

		update.appendUpdate( thisPoolPath.newChild("last-heartbeat"),
				new IntegerStateValue( heartBeat.intValue(), metricLifetime));

		applyUpdates( update);
	}
		
}
