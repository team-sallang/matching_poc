# k6 부하 테스트 실행
# Spring Boot 애플리케이션이 localhost:8080에서 실행 중이어야 합니다
test:
	@if [ -f .env ]; then \
		docker run --rm --env-file .env --add-host "host.docker.internal:host-gateway" -v "$(PWD)/scripts/k6:/scripts" -w /scripts grafana/k6:latest run matching.js; \
	else \
		docker run --rm --add-host "host.docker.internal:host-gateway" -v "$(PWD)/scripts/k6:/scripts" -w /scripts grafana/k6:latest run matching.js; \
	fi

# 테스트 사용자 파일 강제 재생성
regenerate-users:
	@echo "Force regenerating test users file using Docker..."
	@if [ -f .env ]; then \
		echo "Loading environment variables from .env file..."; \
		docker run --rm --env-file .env -v "$(PWD)/scripts/k6:/scripts" -w /scripts node:20-alpine node generate-users.js --force; \
	else \
		echo "No .env file found, using default VU=1000"; \
		docker run --rm -e VU=$${VU:-1000} -v "$(PWD)/scripts/k6:/scripts" -w /scripts node:20-alpine node generate-users.js --force; \
	fi

# Swarm 모드 초기화 (이미 활성화되어 있으면 스킵)
swarm-init:
	@if ! docker info 2>/dev/null | grep -q "Swarm: active"; then \
		echo "Swarm 모드 초기화 중..."; \
		docker swarm init; \
	else \
		echo "Swarm 모드가 이미 활성화되어 있습니다."; \
	fi

# Swarm 스택 배포
swarm-start:
	@echo "Swarm 스택 배포 중..."
	@docker stack deploy -c compose.yml matching-poc
	@echo "완료! 서비스 상태 확인: make swarm-status"

# Swarm 스택 제거
swarm-stop:
	@echo "Swarm 스택 제거 중..."
	@docker stack rm matching-poc
	@echo "완료!"

# Swarm 서비스 상태 확인
swarm-status:
	@docker service ls

help:
	@echo "사용 가능한 명령어:"
	@echo "  make test            - k6 부하 테스트 실행 (docker run 사용)"
	@echo "  make regenerate-users - 테스트 사용자 파일 강제 재생성"
	@echo "  make swarm-init      - Swarm 모드 초기화 (최초 1회)"
	@echo "  make swarm-start     - Swarm 스택 배포"
	@echo "  make swarm-stop      - Swarm 스택 제거"
	@echo "  make swarm-status    - Swarm 서비스 상태 확인"
	@echo ""
	@echo "주의사항:"
	@echo "  - Spring Boot 앱이 localhost:8080에서 실행 중이어야 합니다"
	@echo "  - .env 파일이 있으면 환경변수를 자동으로 로드합니다"
	@echo "  - 테스트 사용자 파일이 없으면 자동으로 생성됩니다"
	@echo "  - Windows에서 make가 없으면 docker 명령어를 직접 사용하세요"
	@echo "  - Swarm 모드에서는 deploy.resources로 리소스 제한이 적용됩니다"
