// Helper methods for parsing HFP AT command codes.  While most AT commands are processed at lower
// levels, some commands are not part of the HFP specification or need to be parsed within Floss for
// whatever reason.

use std::collections::HashMap;

#[derive(Clone, Debug, PartialEq)]
pub enum AtCommandType {
    Set,
    Query,
    Test,
    Execute,
}

pub const AT_COMMAND_DELIMITER_SET: &str = "=";
pub const AT_COMMAND_DELIMITER_QUERY: &str = "?";
pub const AT_COMMAND_DELIMITER_TEST: &str = "=?";

#[derive(Clone, Debug, PartialEq)]
pub enum AtCommandVendor {
    Apple,
    Plantronics,
    Unknown,
}

pub const AT_COMMAND_VENDOR_XAPL: &str = "XAPL";
pub const AT_COMMAND_VENDOR_IPHONEACCEV: &str = "IPHONEACCEV";
pub const AT_COMMAND_VENDOR_IPHONEACCEV_BATTERY: &str = "1";
pub const AT_COMMAND_VENDOR_XEVENT: &str = "XEVENT";

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum AtCommandDataType {
    IPhoneAccevBatteryLevel,
    XeventBatteryLevel,
    XeventBatteryLevelRange,
    XeventEvent,
}

#[derive(Clone)]
pub struct AtCommand {
    // The original, unparsed, AT command
    pub raw: String,
    // The nature of the command according to AT command specifications
    pub at_type: AtCommandType,
    // The actual command being sent (AT+<command>=?)
    pub command: String,
    // Unparsed arguments from the raw command string, in order
    pub raw_args: Option<Vec<String>>,
    // For vendor-specific AT commands
    pub vendor: Option<AtCommandVendor>,
    // For commands with known value types
    pub data: Option<HashMap<AtCommandDataType, String>>,
}

pub const AT_COMMAND_ARG_DELIMITER: &str = ",";

pub fn parse_at_command_data(at_string: String) -> Result<AtCommand, String> {
    // All AT commands should be of the form AT+<command> but may be passed around as +<command> or
    // <command>. We remove those here for convenience.
    let clean_at_string = at_string.strip_prefix("+").unwrap_or(&at_string);
    let clean_at_string = clean_at_string.strip_prefix("AT+").unwrap_or(&clean_at_string);
    if clean_at_string.is_empty() {
        return Err("Cannot parse empty AT command".to_string());
    }
    let at_type = parse_at_command_type(clean_at_string.to_string());
    let at_type_delimiter = match at_type {
        AtCommandType::Set => AT_COMMAND_DELIMITER_SET,
        AtCommandType::Query => AT_COMMAND_DELIMITER_QUERY,
        AtCommandType::Test => AT_COMMAND_DELIMITER_TEST,
        AtCommandType::Execute => "",
    };
    // We want to keep the flow of this method consistent, but AtCommandType::Execute commands do
    // not have arguments. To resolve this we split those commands differently.
    let mut command_parts = match at_type {
        AtCommandType::Execute => clean_at_string.splitn(1, at_type_delimiter),
        _ => clean_at_string.splitn(2, at_type_delimiter),
    };
    let command = match command_parts.next() {
        Some(command) => command,
        // In practice this cannot happen as parse_at_command_type already found the delimiter.
        None => return Err("No command supplied".to_string()),
    };
    let vendor = match command {
        AT_COMMAND_VENDOR_XAPL => Some(AtCommandVendor::Apple),
        AT_COMMAND_VENDOR_IPHONEACCEV => Some(AtCommandVendor::Apple),
        AT_COMMAND_VENDOR_XEVENT => Some(AtCommandVendor::Plantronics),
        _ => None,
    };
    let raw_args = match command_parts.next() {
        Some(arg_string) => {
            if arg_string == "" {
                None
            } else {
                Some(
                    arg_string
                        .split(AT_COMMAND_ARG_DELIMITER)
                        .map(|arg| arg.to_string())
                        .collect::<Vec<String>>(),
                )
            }
        }
        None => None,
    };
    let data = match (raw_args.clone(), command) {
        (Some(args), AT_COMMAND_VENDOR_IPHONEACCEV) => Some(extract_iphoneaccev_data(args)?),
        (Some(args), AT_COMMAND_VENDOR_XEVENT) => Some(extract_xevent_data(args)?),
        (Some(_), _) => None,
        (None, _) => None,
    };
    Ok(AtCommand {
        raw: at_string.to_string(),
        at_type: at_type,
        command: command.to_string(),
        raw_args: raw_args,
        vendor: vendor,
        data: data,
    })
}

pub fn calculate_battery_percent(at_command: AtCommand) -> Result<u32, String> {
    match at_command.data {
        Some(data) => {
            match data.get(&AtCommandDataType::IPhoneAccevBatteryLevel) {
                Some(battery_level) => match battery_level.parse::<u32>() {
                    Ok(level) => return Ok(level * 10),
                    Err(e) => return Err(e.to_string()),
                },
                None => (),
            }
            match data.get(&AtCommandDataType::XeventBatteryLevel) {
                Some(battery_level) => {
                    match data.get(&AtCommandDataType::XeventBatteryLevelRange) {
                        Some(battery_level_range) => {
                            match (battery_level.parse::<u32>(), battery_level_range.parse::<u32>())
                            {
                                (Ok(level), Ok(range)) => {
                                    if level > range {
                                        return Err(format!(
                                            "Invalid battery level {}/{}",
                                            level, range
                                        ));
                                    }
                                    // Mathematically it is not possible to represent anything
                                    // meaningful if there are not at least two options for
                                    // BatteryLevel.
                                    if range < 2 {
                                        return Err(
                                            "BatteryLevelRange must be at least 2".to_string()
                                        );
                                    }
                                    return Ok((f64::from(level) / f64::from(range - 1) * 100.0)
                                        .floor()
                                        as u32);
                                }
                                (Err(e), _) => return Err(e.to_string()),
                                (Ok(_), Err(e)) => return Err(e.to_string()),
                            }
                        }
                        None => return Err("BatteryLevelRange missing".to_string()),
                    }
                }
                None => (),
            }
        }
        None => return Err("No battery data found".to_string()),
    }
    Err("No battery data found".to_string())
}

fn parse_at_command_type(command: String) -> AtCommandType {
    if command.contains(AT_COMMAND_DELIMITER_TEST) {
        return AtCommandType::Test;
    }
    if command.contains(AT_COMMAND_DELIMITER_QUERY) {
        return AtCommandType::Query;
    }
    if command.contains(AT_COMMAND_DELIMITER_SET) {
        return AtCommandType::Set;
    }
    return AtCommandType::Execute;
}

// Format:
// AT+IPHONEACCEV=[NumberOfIndicators],[IndicatorType],[IndicatorValue]
fn extract_iphoneaccev_data(
    args: Vec<String>,
) -> Result<HashMap<AtCommandDataType, String>, String> {
    let num_provided_args: u32 = match args.len().try_into() {
        Ok(num) => num,
        Err(e) => return Err(e.to_string()),
    };
    let mut args = args.iter();
    match args.next() {
        Some(num_claimed) => {
            let num_claimed = match num_claimed.parse::<u32>() {
                Ok(num) => num * 2 + 1,
                Err(e) => return Err(e.to_string()),
            };
            if num_claimed != num_provided_args {
                return Err(format!(
                    "{} indicators were claimed but only {} were found",
                    num_claimed, num_provided_args
                ));
            }
        }
        None => return Err("Expected at least one argument (NumberOfIndicators)".to_string()),
    };
    let mut data = HashMap::new();
    while let Some(indicator_type) = args.next() {
        let indicator_value = args
            .next()
            .ok_or(format!("Failed to find matching value for indicator {}", indicator_type))?;
        // We currently only support battery-related data
        let indicator_type: &str = indicator_type;
        match indicator_type {
            AT_COMMAND_VENDOR_IPHONEACCEV_BATTERY => {
                data.insert(
                    AtCommandDataType::IPhoneAccevBatteryLevel,
                    indicator_value.to_string(),
                );
            }
            _ => continue,
        }
    }
    Ok(data)
}

fn extract_xevent_data(args: Vec<String>) -> Result<HashMap<AtCommandDataType, String>, String> {
    let mut data = HashMap::new();
    let mut args = args.iter();
    let xevent_type = match args.next() {
        Some(event_type) => event_type,
        None => return Err("Expected at least one argument".to_string()),
    };
    data.insert(AtCommandDataType::XeventEvent, xevent_type.to_string());

    // For now we only support BATTERY events.
    if xevent_type != "BATTERY" {
        return Ok(data);
    }
    // Format:
    // AT+XEVENT=BATTERY,[Level],[NumberOfLevel],[MinutesOfTalk],[IsCharging]
    // Battery percentage = 100 * ( Level / (NumberOfLevel - 1 ) )
    match args.next() {
        Some(battery_level) => {
            data.insert(AtCommandDataType::XeventBatteryLevel, battery_level.to_string());
        }
        None => return Err("Expected BatteryLevel argument".to_string()),
    }
    match args.next() {
        Some(battery_level_range) => {
            data.insert(
                AtCommandDataType::XeventBatteryLevelRange,
                battery_level_range.to_string(),
            );
        }
        None => return Err("Expected BatterLevelRange".to_string()),
    }
    // There are more arguments but we don't yet use them.
    Ok(data)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_empty_fails() {
        let at_command = parse_at_command_data("".to_string());
        assert!(at_command.is_err());

        let at_command = parse_at_command_data("+".to_string());
        assert!(at_command.is_err());

        let at_command = parse_at_command_data("AT+".to_string());
        assert!(at_command.is_err());
    }

    #[test]
    fn test_at_string_copied() {
        // A basic command with + preceding
        let at_command = parse_at_command_data("+CMD".to_string()).unwrap();
        assert_eq!(at_command.raw, "+CMD");
    }

    #[test]
    fn test_parse_command_type() {
        let at_command = parse_at_command_data("CMD=".to_string()).unwrap();
        assert_eq!(at_command.at_type, AtCommandType::Set);

        let at_command = parse_at_command_data("CMD?".to_string()).unwrap();
        assert_eq!(at_command.at_type, AtCommandType::Query);

        let at_command = parse_at_command_data("CMD=?".to_string()).unwrap();
        assert_eq!(at_command.at_type, AtCommandType::Test);

        let at_command = parse_at_command_data("CMD".to_string()).unwrap();
        assert_eq!(at_command.at_type, AtCommandType::Execute);
    }

    #[test]
    fn test_parse_command() {
        // A basic command
        let at_command = parse_at_command_data("CMD".to_string()).unwrap();
        assert_eq!(at_command.command, "CMD");

        // A basic command with AT+ preceding
        let at_command = parse_at_command_data("AT+CMD".to_string()).unwrap();
        assert_eq!(at_command.command, "CMD");

        // A basic command with arguments
        let at_command = parse_at_command_data("CMD=a,b,c".to_string()).unwrap();
        assert_eq!(at_command.command, "CMD");
    }

    #[test]
    fn test_parse_args() {
        // No args
        let at_command = parse_at_command_data("AT+CMD".to_string()).unwrap();
        assert_eq!(at_command.raw_args, None);

        // With args
        let at_command = parse_at_command_data("AT+CMD=a,b,c".to_string()).unwrap();
        assert_eq!(
            at_command.raw_args,
            Some(vec!["a".to_string(), "b".to_string(), "c".to_string()])
        );
    }

    #[test]
    fn test_parse_vendor() {
        // With no known vendor
        let at_command = parse_at_command_data("AT+CMD".to_string()).unwrap();
        assert_eq!(at_command.vendor, None);

        // With XAPL
        let at_command = parse_at_command_data("AT+XAPL".to_string()).unwrap();
        assert_eq!(at_command.vendor, Some(AtCommandVendor::Apple));

        // With IPHONEACCEV
        let at_command = parse_at_command_data("AT+IPHONEACCEV".to_string()).unwrap();
        assert_eq!(at_command.vendor, Some(AtCommandVendor::Apple));

        // With XEVENT
        let at_command = parse_at_command_data("AT+XEVENT".to_string()).unwrap();
        assert_eq!(at_command.vendor, Some(AtCommandVendor::Plantronics));
    }

    #[test]
    fn test_parse_iphoneaccev_data() {
        // No args
        let at_command = parse_at_command_data("AT+IPHONEACCEV=".to_string()).unwrap();
        assert_eq!(at_command.data, None);

        // Battery args
        let at_command = parse_at_command_data("AT+IPHONEACCEV=1,1,2".to_string()).unwrap();
        assert_eq!(
            at_command.data,
            Some(HashMap::from([(AtCommandDataType::IPhoneAccevBatteryLevel, "2".to_string())]))
        );

        // Multiple args
        let at_command = parse_at_command_data("AT+IPHONEACCEV=2,2,3,1,2".to_string()).unwrap();
        assert_eq!(
            at_command.data,
            Some(HashMap::from([(AtCommandDataType::IPhoneAccevBatteryLevel, "2".to_string())]))
        );

        // Invalid arg count
        let at_command = parse_at_command_data("AT+IPHONEACCEV=3,1,2".to_string());
        assert!(at_command.is_err());
    }

    #[test]
    fn test_parse_xevent_data() {
        // No args
        let at_command = parse_at_command_data("AT+XEVENT=".to_string()).unwrap();
        assert_eq!(at_command.data, None);

        // No args
        let at_command = parse_at_command_data("AT+XEVENT=DON".to_string()).unwrap();
        assert_eq!(
            at_command.data,
            Some(HashMap::from([(AtCommandDataType::XeventEvent, "DON".to_string())]))
        );
    }

    #[test]
    fn test_parse_xevent_battery_data() {
        // Missing args
        let at_command = parse_at_command_data("AT+XEVENT=BATTERY".to_string());
        assert!(at_command.is_err());

        let at_command = parse_at_command_data("AT+XEVENT=BATTERY,5,9,10,0".to_string()).unwrap();
        assert_eq!(
            at_command.data,
            Some(HashMap::from([
                (AtCommandDataType::XeventEvent, "BATTERY".to_string()),
                (AtCommandDataType::XeventBatteryLevel, "5".to_string()),
                (AtCommandDataType::XeventBatteryLevelRange, "9".to_string()),
            ]))
        );
    }

    #[test]
    fn test_calculate_battery_percent() {
        // Non-battery command
        let at_command = parse_at_command_data("AT+CMD".to_string());
        assert!(!at_command.is_err());
        let battery_level = calculate_battery_percent(at_command.unwrap());
        assert!(battery_level.is_err());

        // Apple - no battery
        let at_command = parse_at_command_data("AT+IPHONEACCEV=1,2,3".to_string());
        assert!(!at_command.is_err());
        let battery_level = calculate_battery_percent(at_command.unwrap());
        assert!(battery_level.is_err());

        // Apple
        let at_command = parse_at_command_data("AT+IPHONEACCEV=1,1,2".to_string());
        assert!(!at_command.is_err());
        let battery_level = calculate_battery_percent(at_command.unwrap()).unwrap();
        assert_eq!(battery_level, 20);

        // Plantronics - missing args
        let at_command = parse_at_command_data("AT+XEVENT=BATTERY".to_string());
        assert!(at_command.is_err());

        // Plantronics
        let at_command = parse_at_command_data("AT+XEVENT=BATTERY,5,11,10,0".to_string());
        assert!(!at_command.is_err());
        let battery_level = calculate_battery_percent(at_command.unwrap()).unwrap();
        assert_eq!(battery_level, 50);
    }
}
