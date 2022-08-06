package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 9:22 PM
 */
@Data
public class Worker {
    private int index;
    private int indexO;
    private int indexD;
    private int latO;
    private int lngO;
    private int latD;
    private int lngD;
    private int capacity;
    private int travelTOD;
}
