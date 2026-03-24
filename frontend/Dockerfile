# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# VITE_API_URL is baked into the build (API base URL as seen by the browser)
ARG VITE_API_URL=https://feeling-okay.com/checkin
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# Serve stage - nginx for static SPA
FROM nginx:alpine

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# SPA routing: fallback unknown paths to index.html
RUN echo 'server { \
    listen 80; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { try_files $uri $uri/ /index.html; } \
  }' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
