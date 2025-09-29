FROM swr.ae-ad-1.g42cloud.com/r100/eclipse-temurin:21_35-jdk

# Specify the JAR file
ENV APP_JAR_FILE git-helper/target/git-helper-*.jar

# Set default locale
ENV LC_ALL en_US.UTF-8
# Set default timezone
ENV TZ=Asia/Dubai

# Set configuration directory and files
RUN mkdir -p /etc/r100
VOLUME ["/etc/r100"]

# Set the timezone
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create a non-root user and group
RUN groupadd -r app && useradd -r -g app -m app

# Optional: Add the new user to the sudoers file if you need sudo access
#RUN echo "app ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Set the working directory
WORKDIR /home/app

# Copy the JAR file
COPY $APP_JAR_FILE app.jar

# Switch to the non-root user
USER app

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]