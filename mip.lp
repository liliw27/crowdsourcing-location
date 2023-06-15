\ENCODING=ISO-8859-1
\Problem name: ilog.cplex

Minimize
 obj: 40 y_s0 + 0.2 w_s0 + 40 y_s1 + 0.2 w_s1 + 40 y_s2 + 0.2 w_s2 + 40 y_s3
      + 0.2 w_s3 + 40 y_s4 + 0.2 w_s4 + 40 y_s5 + 0.2 w_s5 + 40 y_s6 + 0.2 w_s6
      + 40 y_s7 + 0.2 w_s7 + 40 y_s8 + 0.2 w_s8 + 40 y_s9 + 0.2 w_s9
      + 0.02 varQ_0 + 0.02 varQ_1 + 0.02 varQ_2 + 0.02 varQ_3 + 0.02 varQ_4
      + 0.02 varQ_5 + 0.02 varQ_6 + 0.02 varQ_7 + 0.02 varQ_8 + 0.02 varQ_9
      + 0.02 varQ_10 + 0.02 varQ_11 + 0.02 varQ_12 + 0.02 varQ_13
      + 0.02 varQ_14 + 0.02 varQ_15 + 0.02 varQ_16 + 0.02 varQ_17
      + 0.02 varQ_18 + 0.02 varQ_19 + 0.02 varQ_20 + 0.02 varQ_21
      + 0.02 varQ_22 + 0.02 varQ_23 + 0.02 varQ_24 + 0.02 varQ_25
      + 0.02 varQ_26 + 0.02 varQ_27 + 0.02 varQ_28 + 0.02 varQ_29
      + 0.02 varQ_30 + 0.02 varQ_31 + 0.02 varQ_32 + 0.02 varQ_33
      + 0.02 varQ_34 + 0.02 varQ_35 + 0.02 varQ_36 + 0.02 varQ_37
      + 0.02 varQ_38 + 0.02 varQ_39 + 0.02 varQ_40 + 0.02 varQ_41
      + 0.02 varQ_42 + 0.02 varQ_43 + 0.02 varQ_44 + 0.02 varQ_45
      + 0.02 varQ_46 + 0.02 varQ_47 + 0.02 varQ_48 + 0.02 varQ_49
Subject To
 station_capacity_selection_lower_0#0: - 100 y_s0 + w_s0 >= 0
 station_capacity_selection_upper_0#1: - 1000 y_s0 + w_s0 <= 0
 station_capacity_selection_lower_1#2: - 100 y_s1 + w_s1 >= 0
 station_capacity_selection_upper_1#3: - 1000 y_s1 + w_s1 <= 0
 station_capacity_selection_lower_2#4: - 100 y_s2 + w_s2 >= 0
 station_capacity_selection_upper_2#5: - 1000 y_s2 + w_s2 <= 0
 station_capacity_selection_lower_3#6: - 100 y_s3 + w_s3 >= 0
 station_capacity_selection_upper_3#7: - 1000 y_s3 + w_s3 <= 0
 station_capacity_selection_lower_4#8: - 100 y_s4 + w_s4 >= 0
 station_capacity_selection_upper_4#9: - 1000 y_s4 + w_s4 <= 0
 station_capacity_selection_lower_5#10: - 100 y_s5 + w_s5 >= 0
 station_capacity_selection_upper_5#11: - 1000 y_s5 + w_s5 <= 0
 station_capacity_selection_lower_6#12: - 100 y_s6 + w_s6 >= 0
 station_capacity_selection_upper_6#13: - 1000 y_s6 + w_s6 <= 0
 station_capacity_selection_lower_7#14: - 100 y_s7 + w_s7 >= 0
 station_capacity_selection_upper_7#15: - 1000 y_s7 + w_s7 <= 0
 station_capacity_selection_lower_8#16: - 100 y_s8 + w_s8 >= 0
 station_capacity_selection_upper_8#17: - 1000 y_s8 + w_s8 <= 0
 station_capacity_selection_lower_9#18: - 100 y_s9 + w_s9 >= 0
 station_capacity_selection_upper_9#19: - 1000 y_s9 + w_s9 <= 0
 at_least_one_location#20:       y_s0 + y_s1 + y_s2 + y_s3 + y_s4 + y_s5 + y_s6
                                 + y_s7 + y_s8 + y_s9 >= 1
Bounds
 0 <= y_s0 <= 1
 0 <= w_s0 <= 1000
 0 <= y_s1 <= 1
 0 <= w_s1 <= 1000
 0 <= y_s2 <= 1
 0 <= w_s2 <= 1000
 0 <= y_s3 <= 1
 0 <= w_s3 <= 1000
 0 <= y_s4 <= 1
 0 <= w_s4 <= 1000
 0 <= y_s5 <= 1
 0 <= w_s5 <= 1000
 0 <= y_s6 <= 1
 0 <= w_s6 <= 1000
 0 <= y_s7 <= 1
 0 <= w_s7 <= 1000
 0 <= y_s8 <= 1
 0 <= w_s8 <= 1000
 0 <= y_s9 <= 1
 0 <= w_s9 <= 1000
 0 <= varQ_0 <= 2147483647
 0 <= varQ_1 <= 2147483647
 0 <= varQ_2 <= 2147483647
 0 <= varQ_3 <= 2147483647
 0 <= varQ_4 <= 2147483647
 0 <= varQ_5 <= 2147483647
 0 <= varQ_6 <= 2147483647
 0 <= varQ_7 <= 2147483647
 0 <= varQ_8 <= 2147483647
 0 <= varQ_9 <= 2147483647
 0 <= varQ_10 <= 2147483647
 0 <= varQ_11 <= 2147483647
 0 <= varQ_12 <= 2147483647
 0 <= varQ_13 <= 2147483647
 0 <= varQ_14 <= 2147483647
 0 <= varQ_15 <= 2147483647
 0 <= varQ_16 <= 2147483647
 0 <= varQ_17 <= 2147483647
 0 <= varQ_18 <= 2147483647
 0 <= varQ_19 <= 2147483647
 0 <= varQ_20 <= 2147483647
 0 <= varQ_21 <= 2147483647
 0 <= varQ_22 <= 2147483647
 0 <= varQ_23 <= 2147483647
 0 <= varQ_24 <= 2147483647
 0 <= varQ_25 <= 2147483647
 0 <= varQ_26 <= 2147483647
 0 <= varQ_27 <= 2147483647
 0 <= varQ_28 <= 2147483647
 0 <= varQ_29 <= 2147483647
 0 <= varQ_30 <= 2147483647
 0 <= varQ_31 <= 2147483647
 0 <= varQ_32 <= 2147483647
 0 <= varQ_33 <= 2147483647
 0 <= varQ_34 <= 2147483647
 0 <= varQ_35 <= 2147483647
 0 <= varQ_36 <= 2147483647
 0 <= varQ_37 <= 2147483647
 0 <= varQ_38 <= 2147483647
 0 <= varQ_39 <= 2147483647
 0 <= varQ_40 <= 2147483647
 0 <= varQ_41 <= 2147483647
 0 <= varQ_42 <= 2147483647
 0 <= varQ_43 <= 2147483647
 0 <= varQ_44 <= 2147483647
 0 <= varQ_45 <= 2147483647
 0 <= varQ_46 <= 2147483647
 0 <= varQ_47 <= 2147483647
 0 <= varQ_48 <= 2147483647
 0 <= varQ_49 <= 2147483647
Binaries
 y_s0  y_s1  y_s2  y_s3  y_s4  y_s5  y_s6  y_s7  y_s8  y_s9 
End
