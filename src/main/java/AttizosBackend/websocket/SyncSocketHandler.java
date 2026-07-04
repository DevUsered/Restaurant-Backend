package AttizosBackend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class SyncSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sesionesActivas = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sesionesActivas.add(session);
        System.out.println("Nueva caja conectada al tunel. ID: "+session.getId());
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        sesionesActivas.remove(session);
        System.out.println("Caja desconectada del tunel. ID: "+session.getId());
    }
    public void notificarAClientes(String mensaje){
        for(WebSocketSession session : sesionesActivas){
            if(session.isOpen()){
                try{
                    session.sendMessage(new TextMessage(mensaje));
                } catch (IOException e) {
                    System.out.println("Error al enviar mensaje: "+ session.getId());
                }
            }
        }
    }
}
