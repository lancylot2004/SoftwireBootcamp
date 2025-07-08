from configparser import ConfigParser

config = ConfigParser()
config.read("config.ini")

EMAIL = config["credentials"]["email"]
PASSWORD = config["credentials"]["password"]
