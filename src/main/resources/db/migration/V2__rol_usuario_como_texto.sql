-- Unifica el tipo de usuarios.rol en todas las bases (algunas lo tenían como ENUM):
-- como texto, agregar un rol nuevo no exige cambiar la tabla.
ALTER TABLE usuarios MODIFY rol varchar(30) NOT NULL;
