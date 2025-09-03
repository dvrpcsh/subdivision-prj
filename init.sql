-- 'nottori_db' 데이터베이스에 대해 'admin' 사용자가 모든 IP('%')에서 접속할 수 있도록 모든 권한을 부여합니다.
GRANT ALL PRIVILEGES ON nottori_db.* TO 'admin'@'%';
-- 권한 변경사항을 즉시 적용합니다.
FLUSH PRIVILEGES;
