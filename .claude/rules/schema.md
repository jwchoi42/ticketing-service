```
Table users {
  id integer [primary key]
  email varchar
  password varchar
}

Ref: users.id < reservations.user_id

Table payments {
  id integer [primary key]
  reservation_id integer
  amount integer
  method varchar
  status varchar
  payment_gateway_provider varchar
  payment_transaction_id varchar
  created_at timestamp
  paid_at timestamp  
}

Ref: reservations.id < payments.reservation_id

Table matches {
  id integer [primary key]
  stadium varchar
  home_team varchar
  away_team varchar
  date_time timestamp
}

Ref: matches.id < reservations.match_id

Table reservations {
  id integer [primary key]
  user_id integer
  match_id integer
  status varchar 
}

Table seat_allocations {
  
  id integer [primary key]
  
  reservation_id integer 
  
  match_id integer
  seat_id integer  

  Indexes {
    (match_id, seat_id) [unique]
  }

  status varchar
  hold_expires_at timestamp   
}

Ref: matches.id < seat_allocations.match_id
Ref: seats.id < seat_allocations.seat_id
Ref: reservations.id < seat_allocations.reservation_id

//
 
Table seats {
  block_id integer
  id integer [primary key]
}

Ref: blocks.id < seats.block_id

Table blocks {
  section_id integer
  id integer [primary key]
}

Ref: sections.id < blocks.section_id

Table sections {
  area_id integer
  id integer [primary key]
  type varchar
}

Ref: areas.id < sections.area_id

Table areas {
  id integer [primary key]
  type varchar
}
```