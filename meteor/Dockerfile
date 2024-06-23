FROM node:14.21.3

ENV METEOR_ALLOW_SUPERUSER=true
ENV ROOT_URL="http://localhost:3000"

RUN curl "https://install.meteor.com/?release=2.16" | sh

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN meteor npm ci || meteor npm install
RUN meteor build /usr/src/build

WORKDIR /usr/src/build

EXPOSE 3000
CMD ["node","main.js"]