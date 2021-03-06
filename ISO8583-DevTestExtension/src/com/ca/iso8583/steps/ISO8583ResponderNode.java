package com.ca.iso8583.steps;

import java.util.List;

import com.ca.iso8583.clientserver.CallbackAction;
import com.ca.iso8583.clientserver.ISOConnection;
import com.ca.iso8583.exception.ParseException;
import com.ca.iso8583.helper.PayloadMessageConfig;
import com.ca.iso8583.protocol.ISOMessage;
import com.itko.lisa.test.TestExec;
import com.itko.lisa.test.TestRunException;
import com.itko.lisa.vse.stateful.model.TransientResponse;


public class ISO8583ResponderNode extends GenericConnectionChooserNode {

	@Override
	public String getTypeName() throws Exception {
		return "ISO8583 Responder";
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void execute(TestExec testExec) throws TestRunException {
		try {
			ISOConnection isoConnection = (ISOConnection) testExec.getStateValue(getConnectionName());
			
			Object responseObj = testExec.getStateObject("lisa.vse.response");
			TransientResponse response = (TransientResponse) ((List) responseObj).get(0);
			
			String parsedXML = testExec.parseInState(response.getBodyAsString());
			
			final TestExec te = testExec;
			final PayloadMessageConfig payloadMessageConfig = new PayloadMessageConfig(parsedXML);
			ISOMessage isoMessage = new ISOMessage(payloadMessageConfig.getMessageVO());
			
			byte[] data = payloadMessageConfig.getIsoConfig().getDelimiter().preparePayload(isoMessage, payloadMessageConfig.getIsoConfig());
			
			isoConnection.setIsoConfig(payloadMessageConfig.getIsoConfig());
			isoConnection.setCallback(new CallbackAction() {

				@Override
				public void dataReceived(byte[] data) throws ParseException {
					payloadMessageConfig.updateFromPayload(data);
					te.setLastResponse(payloadMessageConfig.getXML());
				}

				@Override
				public void log(String log) { }

				@Override
				public void end() {
					if (te.getLastResponse() == null || te.getLastResponse().equals(""))
						te.setLastResponse("");
				}
				
			});
			
			if (!isoConnection.isConnected()) isoConnection.connect();
			isoConnection.sendBytes(data, payloadMessageConfig.getISOTestVO().isRequestSync(), false);
		}
		catch (Exception x) {
			throw new TestRunException(x.getMessage(), x);
		}
	}

}
