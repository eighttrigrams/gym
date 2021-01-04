(ns engine.ui
  (:import (javax.swing Timer JFrame)
           (java.awt Canvas)
           (java.awt GraphicsEnvironment)))

(defn create-frame-listener
  [paint]
  (proxy [java.awt.event.ActionListener] []
         (actionPerformed
          [_]
          (paint))))

(defn create-key-listener [atom:keys]
  (proxy [java.awt.event.KeyListener] []
         (keyPressed [event]
                     (if (not (nil? event))
                       (let [key       (.getKeyChar event)]
                         (swap! atom:keys #(conj % key)))))
         (keyReleased [event]
                      (if (not (nil? event))
                        (let [key       (.getKeyChar event)]
                          (swap! atom:keys #(disj % key)))))
         (keyTyped [event])))

(defn create-physics-listener
  [on-step tick-in-ms]
  (proxy [java.awt.event.ActionListener] []
         (actionPerformed
          [_]
          (on-step tick-in-ms))))

(defn looping [frame-listener physics-listener fps ticks-per-second]
  (let [tick-in-ms            (/ 100 ticks-per-second)
        frame-in-ms           (/ 100 fps)
        physics-listener      (physics-listener tick-in-ms)
        frame-timer           (Timer. frame-in-ms frame-listener)
        physics-timer         (Timer. tick-in-ms physics-listener)]
    (.start frame-timer)
    (.start physics-timer)))

(defn- init-buffered-image
  [size]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gc (.getDefaultScreenDevice ge)
        gd (.getDefaultConfiguration gc)]
    (.createCompatibleImage gd size size)))

(defn init-jframe [size key-listener]
  (let [app          (JFrame.)
        canvas       (Canvas.)]
    (.setTitle app "Run")
    (.setSize canvas size size)
    (.setDefaultCloseOperation app JFrame/EXIT_ON_CLOSE)
    (.addKeyListener canvas key-listener)
    (.add app canvas)
    (.pack app)
    (.setVisible app true)
    (.createBufferStrategy canvas 2)
    [(.getBufferStrategy canvas)
     (init-buffered-image size)]))
