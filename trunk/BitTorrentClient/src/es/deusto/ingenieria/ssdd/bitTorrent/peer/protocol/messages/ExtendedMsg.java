package es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages;

import java.io.ByteArrayOutputStream;

import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class ExtendedMsg extends PeerProtocolMessage {

	public ExtendedMsg(int messageId, byte[] message) {
		super(Type.EXTENDED);
		super.setLength(ToolKit.intToBigEndianBytes(2 + message.length,
				new byte[4], 0));
		this.updatePayload(messageId, message);
	}

	private void updatePayload(int messageId, byte[] message) {
		try {
			ByteArrayOutputStream payload = new ByteArrayOutputStream();

			payload.write(messageId);

			payload.write(message);

			super.setPayload(payload.toByteArray());
		} catch (Exception ex) {
			System.out.println("# Error updating BitfieldMsg payload: "
					+ ex.getMessage());
		}
	}
}