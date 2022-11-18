package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:41 PM
 */
@Data
public class Station extends StationCandidate {
   private double capacity;
   public int hashCode() {
      return this.getNodeIndex();
   }

   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (!(o instanceof StationCandidate))
         return false;
      StationCandidate other = (StationCandidate) o;
      return (this.getNodeIndex() == other.getNodeIndex());
   }

   public String toString(){
      String s = "Station: " + this.getIndex() +"capacity: " +capacity+"\n";
      return s;

   }
}
