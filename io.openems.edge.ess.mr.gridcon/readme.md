# MR Gridcon

## Main state machine

~~~~
          +-----------------------------------------------------------------------------+
          |                                                                             |
          v                                                                             |
+---------+---------+       +-------------+      +--------------------+         +-------+------+
|                   |       |             |      |                    |         |              |
|   Going On-Grid   +------>+   On-Grid   +----->+   Going Off-Grid   +-------->+   Off-Grid   |
|                   |       |             |      |                    |         |              |
+-------------------+       +-------------+      +--------------------+         +--------------+


                          from all             to all
                          +-----------+        +---------------+
                          |           |        |               |
                          |   Error   |        |   Undefined   |
                          |           |        |               |
                          +-----------+        +---------------+
~~~~

## Error state machine

// TODO

## Going On-Grid state machine

// TODO

## On-Grid state machine

// TODO

## Going Off-Grid state machine

// TODO

## Off-Grid state machine