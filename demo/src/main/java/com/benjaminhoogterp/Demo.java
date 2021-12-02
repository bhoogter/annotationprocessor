package com.benjaminhoogterp;

public class Demo {
    @WrapMethod
    public int demo() {
        System.out.println("Begin Normal Function handler...");

        for(int i = 0; i < 10; i++) {
            System.out.println("Some normal operations: " + i);
        }

        System.out.println("Prepare for return...");
        return -1;
    }
}
