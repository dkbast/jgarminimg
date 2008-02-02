create sequence POI_SERIAL;
create table POI (ID integer primary key default nextval('POI_SERIAL'), MAP integer not null, LEVEL integer not null, TYPE integer not null, SUB_TYPE integer not null, NAME text, STREET_NUMBER text, STREET text, CITY text, ZIP text, PHONE text);
select AddGeometryColumn('poi', 'position', (select srid from spatial_ref_sys where srtext like 'GEOGCS["WGS 84%'), 'POINT', 2 );

create sequence POLYLINE_SERIAL;
create table POLYLINE (ID integer primary key default nextval('POLYLINE_SERIAL'), MAP integer not null, LEVEL integer not null, TYPE integer not null, NAME text);
select AddGeometryColumn('polyline', 'path', (select srid from spatial_ref_sys where srtext like 'GEOGCS["WGS 84%'), 'LINESTRING', 2 );

create sequence POLYGON_SERIAL;
create table POLYGON (ID integer primary key default nextval('POLYGON_SERIAL'), MAP integer not null, LEVEL integer not null, TYPE integer not null, NAME text);
select AddGeometryColumn('polygon', 'contour', (select srid from spatial_ref_sys where srtext like 'GEOGCS["WGS 84%'), 'POLYGON', 2 );

# after insertion of the data =========================

update polygon set contour=st_buffer(contour, 0.0) where not st_issimple(contour);

select AddGeometryColumn('polygon', 'contour_fixed', (select srid from spatial_ref_sys where srtext like 'GEOGCS["WGS 84%'), 'POLYGON', 2 );
update polygon set contour_fixed=(SELECT ST_BuildArea(geom) AS geom
                                  FROM
                                    (
                                       -- Re-node the linear ring
                                       SELECT St_Union(geom, St_Startpoint(geom)) AS geom
                                       FROM
                                       (
                                          -- The exterior ring of your polygon
                                          SELECT ST_ExteriorRing(contour) AS geom
                                       ) AS ring
                                    ) AS fixed_ring) where not st_isvalid(contour);
update polygon set contour_fixed=contour where contour_fixed is null;

create index POLYLINE_PATH on POLYLINE using gist (PATH GIST_GEOMETRY_OPS);
create index polyline_level on polyline (level);
create index polyline_type on polyline (type);
create index polyline_map on polyline (map);
create index POI_POSITION on POI using gist (POSITION GIST_GEOMETRY_OPS);
create index poi_level on poi (level);
create index poi_map on poi (map);
create index POLYGON_CONTOUR on POLYGON using gist (CONTOUR GIST_GEOMETRY_OPS);
create index polygon_level on polygon (level);
create index polygon_type on polygon (type);
create index polygon_map on polygon (map);

vacuum verbose analyze;

create sequence JUNCTION_SERIAL;
create table JUNCTION (ID integer primary key default nextval('JUNCTION_SERIAL'));
select AddGeometryColumn('junction', 'position', (select srid from spatial_ref_sys where srtext like 'GEOGCS["WGS 84%'), 'POINT', 2 );

insert into junction (position)
    select point from (select st_pointn(path, st_numpoints(path)) as point from polyline where level=0 and (type<=12 or type=22 or (type>=256 and type<=268) or type=278)
                         union
                       select st_startpoint(path) as point from polyline where level=0 and (type<=12 or type=22 or (type>=256 and type<=268) or type=278)
                      ) as points
                 group by point;
create index junction_position on junction using gist (position GIST_GEOMETRY_OPS);

vacuum analyze;

create table junction_polyline (start_id integer not null, end_id integer not null, polyline_id integer not null, length real not null, time real);
insert into junction_polyline (start_id, end_id, polyline_id, length)
    select j1.id, j2.id, p.id, st_length2d(st_transform(p.path,21781))
        from polyline p, junction j1, junction j2 
        where p.path&&j1.position and
              p.path&&j2.position and
              p.level=0 and (p.type<=12 or p.type=22 or (p.type>=256 and p.type<=268) or p.type=278)
              st_startpoint(p.path)=j1.position and
              st_pointn(path, st_numpoints(path))=j2.position;
                            
create index junction_polyline_id on junction_polyline (polyline_id);

update junction_polyline set time=length/(120/3.6) where polyline_id in (select id from polyline where type=1 or type=257);
update junction_polyline set time=length/(120/3.6) where polyline_id in (select id from polyline where type=2 or type=258);
update junction_polyline set time=length/(80/3.6) where polyline_id in (select id from polyline where type=3 or type=259);
update junction_polyline set time=length/(80/3.6) where polyline_id in (select id from polyline where type=4 or type=260);
update junction_polyline set time=length/(50/3.6) where polyline_id in (select id from polyline where type=5 or type=261);
update junction_polyline set time=length/(50/3.6) where polyline_id in (select id from polyline where type=6 or type=262);
update junction_polyline set time=length/(10/3.6) where polyline_id in (select id from polyline where type=10 or type=266);
update junction_polyline set time=length/(5/3.6) where polyline_id in (select id from polyline where type=22 or type=278);
vacuum analyze junction_polyline;