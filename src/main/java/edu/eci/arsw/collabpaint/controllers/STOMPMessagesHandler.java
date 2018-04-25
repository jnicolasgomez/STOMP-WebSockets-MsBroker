package edu.eci.arsw.collabpaint.controllers;

import edu.eci.arsw.collabpaint.model.Point;
import edu.eci.arsw.collabpaint.util.JedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Controller
public class STOMPMessagesHandler {

    private ConcurrentHashMap<String, ArrayList<Point>> polygonpts=  new ConcurrentHashMap<>();
    private ArrayList<Integer> coordX = new ArrayList<>();
    private ArrayList<Integer> coordY = new ArrayList<>();

    @Autowired
    SimpMessagingTemplate msgt;

    @MessageMapping("/newpoint.{numdibujo}")
    public void handlePointEvent(Point pt, @DestinationVariable String numdibujo) throws Exception {
        Jedis jedis = JedisUtil.getPool().getResource();
        jedis.watch("x", "y");
        Transaction t = jedis.multi();
        t.set("x", "y");
        System.out.println("SIZEEEEEE:   -  " + t.exec().size());
        String[] valx;
        valx= new String[]{String.valueOf(pt.getX())};
        String[] valy;
        valy= new String[]{String.valueOf(pt.getY())};
        jedis.rpush("x",valx);
        jedis.rpush("y", valy);
        List<Object> res=t.exec();
        System.out.println(res.size());
        jedis.close();
        System.out.println("Nuevo punto recibido en el servidor!:"+pt);
        if(!polygonpts.containsKey(numdibujo)){
            polygonpts.put(numdibujo, new ArrayList<>());
            polygonpts.get(numdibujo).add(pt);
        }else{
            polygonpts.get(numdibujo).add(pt);
        }

        if (polygonpts.get(numdibujo).size() == 4) {
            msgt.convertAndSend("/topic/newpolygon."+numdibujo, polygonpts.get(numdibujo));
            polygonpts.get(numdibujo).clear();
        }
        msgt.convertAndSend("/topic/newpoint."+numdibujo, pt);
    }


}
