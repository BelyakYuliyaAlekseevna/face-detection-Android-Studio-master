package org.opencv.samples.facedetect;

import java.util.ArrayList;

public class Product {

    public char 	            state='0' ;
    public String               dir=null ;
    public boolean              done =false;
    public int[][]              tracks;
    public int 	                i;


    public char getState() {
        return this.state;
    }


    public String getDir() {
        return this.dir;
    }

    public void setDone() {
        this.done = true;
    }


    public boolean going_UP(int mid_start, int mid_end, ArrayList <Integer> oneProduct) {
        if ( oneProduct.size()>1) {
            if (this.state =='0') {
                if (oneProduct.get(oneProduct.size()-2) < mid_start && oneProduct.get(oneProduct.size()-1) >= mid_start) {
                    state = '1';
                    this.dir = "up";
                    return true; }
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
        return done;
    }


    public boolean going_DOWN(int mid_start, int mid_end, ArrayList <Integer> oneProduct) {
        if ( oneProduct.size()>1) {
            if (this.state == '0') {
                if (oneProduct.get(oneProduct.size()-2) > mid_start && oneProduct.get(oneProduct.size()-1) <= mid_end) {
                    state = '1';
                    this.dir = "down";
                    return true; }
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
        return done;
    }

}
