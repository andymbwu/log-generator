import { AddIcon, SmallCloseIcon } from '@chakra-ui/icons';
import {
  Button,
  Divider,
  FormControl,
  FormErrorMessage,
  FormLabel,
  HStack,
  IconButton,
  Input,
  InputGroup,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  NumberInput,
  NumberInputField,
  Text,
  Textarea,
  VStack,
  Wrap,
  WrapItem,
} from '@chakra-ui/react';
import { isNaN } from 'formik';
import React, { useCallback, useEffect, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

function CustomLogsModal({
  isOpen,
  onClose,
  field,
  form,
  meta,
  defaultCustomLog,
}) {
  const [customLogsText, setCustomLogsText] = useState('');
  const [customLogsTextError, setCustomLogsTextError] = useState(null);

  function parseKeyValue(key, value, emptyValue) {
    if (value === '') return emptyValue;
    else if (!['pathToFile', 'fileSHA256'].includes(key))
      return isNaN(Number(value)) ? value : Number(value);
    else return value;
  }

  const updateCustomLogs = useCallback(() => {
    function replacer(key, value) {
      if (key === 'id') return undefined;
      return parseKeyValue(key, value, undefined);
    }

    if (field.value.length === 0) {
      setCustomLogsText('');
    } else {
      setCustomLogsText(JSON.stringify(field.value, replacer, 2));
    }
    setCustomLogsTextError(null);
  }, [field.value]);

  useEffect(() => {
    updateCustomLogs();
  }, [updateCustomLogs]);

  function onCloseModal() {
    onClose();
    updateCustomLogs();
  }

  function loadCustomLogs() {
    let error = null;

    try {
      // parse text
      const customLogs = JSON.parse(customLogsText);

      // validate object is an array
      if (!Array.isArray(customLogs)) throw Error('JSON must be an array');

      // parse for each log
      customLogs.forEach(function (customLog, index) {
        // reset log
        customLogs[index] = {};

        // set frequency to default or number entered
        if (isNaN(Number(customLog.frequency))) {
          customLogs[index].frequency = defaultCustomLog.frequency;
        } else {
          customLogs[index].frequency = customLog.frequency;
        }

        // add all given fields to standard log fields
        customLogs[index].fields = {
          ...defaultCustomLog.fields,
          ...customLog.fields,
        };

        // set random id to custom log
        customLogs[index].id = uuidv4();
      });

      // set customLogs field
      form.setFieldValue(field.name, customLogs);

      console.log(JSON.stringify(customLogs, null, 2));
    } catch (err) {
      error = err.message;
    } finally {
      setCustomLogsTextError(error);
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onCloseModal} size="6xl" closeOnEsc={false}>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Custom Logs</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack align="start">
            {field.value.map((log, index) => (
              <VStack key={log.id}>
                <HStack>
                  <Wrap spacingX="1em" spacingY="0.5em" gap="1em" pt="0.5em">
                    <WrapItem>
                      <HStack>
                        <Text>frequency:</Text>
                        <FormControl isInvalid={meta.error}>
                          <InputGroup maxW="8em">
                            <NumberInput
                              min={0}
                              max={1}
                              value={log.frequency}
                              onChange={val =>
                                form.setFieldValue(
                                  `customLogs.${index}.frequency`,
                                  val
                                )
                              }
                            >
                              <NumberInputField
                                h="2em"
                                w="6em"
                                placeholder="0"
                              />
                            </NumberInput>
                          </InputGroup>
                        </FormControl>
                      </HStack>
                    </WrapItem>
                    {Object.keys(log.fields).map(keyName => (
                      <WrapItem key={`${log.id}.${keyName}`}>
                        <HStack>
                          <Text>{keyName}:</Text>
                          <FormControl>
                            <Input
                              defaultValue={log.fields[keyName]}
                              //change values onBlur to improve rendering performance
                              onBlur={event =>
                                form.setFieldValue(
                                  `customLogs.${index}.fields.${keyName}`,
                                  parseKeyValue(keyName, event.target.value, '')
                                )
                              }
                              placeholder="random"
                              h="2em"
                              w="8em"
                            />
                          </FormControl>
                        </HStack>
                      </WrapItem>
                    ))}
                  </Wrap>
                  <IconButton
                    icon={<SmallCloseIcon />}
                    variant="ghost"
                    onClick={() => {
                      form.setFieldValue(
                        field.name,
                        field.value.filter((_, i) => i !== index)
                      );
                    }}
                  />
                </HStack>
                <Divider pt="0.5em" />
              </VStack>
            ))}
            <FormErrorMessage>{meta.error}</FormErrorMessage>
            <IconButton
              icon={<AddIcon />}
              size="sm"
              onClick={() =>
                form.setFieldValue(field.name, [
                  ...field.value,
                  {
                    ...defaultCustomLog,
                    id: uuidv4(),
                  },
                ])
              }
            />
            <FormControl pt="0.75em" isInvalid={customLogsTextError}>
              <FormLabel>Load custom loglines</FormLabel>
              <Textarea
                placeholder="Insert custom logs in JSON format"
                w="85%"
                h="13em"
                value={customLogsText}
                onChange={e => setCustomLogsText(e.target.value)}
              />
              <FormErrorMessage>{customLogsTextError}</FormErrorMessage>
            </FormControl>
            <HStack spacing="1em" pt="0.1em">
              <Button colorScheme="green" onClick={loadCustomLogs}>
                Load
              </Button>
              <Button variant="ghost" onClick={updateCustomLogs}>
                Reset
              </Button>
            </HStack>
          </VStack>
        </ModalBody>

        <ModalFooter>
          <Button
            variant="ghost"
            mr={3}
            onClick={() => form.setFieldValue(field.name, [])}
          >
            Reset
          </Button>
          <Button colorScheme="blue" onClick={onClose}>
            Close
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}

export default CustomLogsModal;
