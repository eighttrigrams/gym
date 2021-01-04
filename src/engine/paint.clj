(ns engine.paint
  (:import (java.awt Graphics2D)
           (java.awt.geom AffineTransform)))

(defn paint-background [^Graphics2D g2d]
  (.setColor g2d (java.awt.Color. 255 255 255))
  (.fillRect g2d 0 0 400 400))

(defn paint-rectangle
  [^Graphics2D g2d
   {x        :x
    y        :y
    rotation :rotation
    {type   :type
     width  :width
     height :height}
    :properties}]
  (.setColor g2d (java.awt.Color. 0 0 0))
  (let [ot (.getTransform g2d)
        at (AffineTransform.)]
    (.translate at x (- y))
    (.rotate at rotation)
    (.transform g2d at)
    (.fillRect g2d
               (- (/ width 2.0))
               (- (/ height 2.0))
               width
               height)
    (.setTransform g2d ot)))

;; TODO use multimethod dispatch
(defn paint-body
  [^Graphics2D g2d]
  (fn [engine-body]
    (when (= :rectangle (:type (:properties engine-body)))
      (paint-rectangle g2d engine-body))))

(defn paint
  [^java.awt.image.BufferStrategy buffer-strategy
   ^java.awt.image.BufferedImage buffered-image]
  (fn
    [bodies]
    (let [graphics (.getDrawGraphics buffer-strategy)
          g2d      (.createGraphics buffered-image)]
      (try
        (paint-background g2d)
        (mapv (paint-body g2d) bodies)
        (.drawImage graphics buffered-image 0 0 nil)
        (if (not (.contentsLost buffer-strategy))
          (.show buffer-strategy))
        (finally
          (if graphics (.dispose graphics))
          (if g2d (.dispose g2d)))))))
